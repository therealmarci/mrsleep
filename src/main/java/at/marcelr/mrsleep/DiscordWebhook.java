package at.marcelr.mrsleep;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.configuration.ConfigurationSection;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

/**
 * Sends log events to a Discord webhook. Requests run one after another on a background thread,
 * so the main thread never waits and Discord's rate limit (HTTP 429) can be respected.
 */
public final class DiscordWebhook {

    private static final int MAX_RETRIES = 3;

    private final MRSleep plugin;
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "MRsleep-Discord");
        thread.setDaemon(true);
        return thread;
    });

    private volatile Config config = Config.DISABLED;

    private record Event(boolean enabled, String message, int color) {
    }

    private record Config(boolean enabled, URI url, String username, String avatarUrl,
                          boolean embeds, String footer, Map<String, Event> events) {
        static final Config DISABLED = new Config(false, null, "", "", false, "", Map.of());
    }

    public DiscordWebhook(MRSleep plugin) {
        this.plugin = plugin;
    }

    public void load(ConfigurationSection section) {
        if (section == null || !section.getBoolean("enabled", false)) {
            config = Config.DISABLED;
            return;
        }
        String rawUrl = section.getString("webhook-url", "").trim();
        if (!rawUrl.startsWith("https://discord.com/api/webhooks/")
                && !rawUrl.startsWith("https://discordapp.com/api/webhooks/")
                && !rawUrl.startsWith("https://canary.discord.com/api/webhooks/")
                && !rawUrl.startsWith("https://ptb.discord.com/api/webhooks/")) {
            plugin.getLogger().warning("Discord-Webhook ist aktiviert, aber discord.webhook-url ist keine gültige Webhook-URL.");
            config = Config.DISABLED;
            return;
        }

        Map<String, Event> events = new HashMap<>();
        ConfigurationSection eventSection = section.getConfigurationSection("events");
        if (eventSection != null) {
            for (String key : eventSection.getKeys(false)) {
                ConfigurationSection e = eventSection.getConfigurationSection(key);
                if (e != null) {
                    events.put(key, new Event(
                            e.getBoolean("enabled", true),
                            e.getString("message", ""),
                            parseColor(e.getString("color", "#5865F2"))));
                }
            }
        }
        config = new Config(true, URI.create(rawUrl),
                section.getString("username", "MRsleep"),
                section.getString("avatar-url", ""),
                section.getBoolean("use-embeds", true),
                section.getString("footer", ""),
                Map.copyOf(events));
    }

    /**
     * Sends the configured message of an event. {@code placeholders} alternates key and value,
     * e.g. {@code log("night-skipped", "world", "world", "sleeping", "2")} for {@code {world}} and {@code {sleeping}}.
     */
    public void log(String eventKey, Object... placeholders) {
        Config current = config;
        if (!current.enabled()) {
            return;
        }
        Event event = current.events().get(eventKey);
        if (event == null || !event.enabled() || event.message().isEmpty()) {
            return;
        }
        String text = event.message();
        for (int i = 0; i + 1 < placeholders.length; i += 2) {
            text = text.replace("{" + placeholders[i] + "}", escapeMarkdown(String.valueOf(placeholders[i + 1])));
        }
        String payload = buildPayload(current, event, text);
        executor.execute(() -> send(current.url(), payload));
    }

    private static String buildPayload(Config config, Event event, String text) {
        JsonObject json = new JsonObject();
        if (!config.username().isEmpty()) {
            json.addProperty("username", config.username());
        }
        if (!config.avatarUrl().isEmpty()) {
            json.addProperty("avatar_url", config.avatarUrl());
        }
        // Never ping anyone, even if a message contains @everyone or a mention.
        JsonObject mentions = new JsonObject();
        mentions.add("parse", new JsonArray());
        json.add("allowed_mentions", mentions);

        if (config.embeds()) {
            JsonObject embed = new JsonObject();
            embed.addProperty("description", text);
            embed.addProperty("color", event.color());
            embed.addProperty("timestamp", Instant.now().toString());
            if (!config.footer().isEmpty()) {
                JsonObject footer = new JsonObject();
                footer.addProperty("text", config.footer());
                embed.add("footer", footer);
            }
            JsonArray embeds = new JsonArray();
            embeds.add(embed);
            json.add("embeds", embeds);
        } else {
            json.addProperty("content", text);
        }
        return json.toString();
    }

    private void send(URI url, String payload) {
        HttpRequest request = HttpRequest.newBuilder(url)
                .timeout(Duration.ofSeconds(15))
                .header("Content-Type", "application/json")
                .header("User-Agent", "MRsleep/" + plugin.getPluginMeta().getVersion())
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .build();
        try {
            for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                int status = response.statusCode();
                if (status / 100 == 2) {
                    return;
                }
                if (status != 429) {
                    plugin.getLogger().warning("Discord-Webhook antwortete mit HTTP " + status + ": " + response.body());
                    return;
                }
                Thread.sleep(retryAfterMillis(response));
            }
            plugin.getLogger().warning("Discord-Webhook: Rate-Limit, Nachricht verworfen.");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Discord-Webhook konnte nicht gesendet werden: " + e.getMessage());
        }
    }

    private static long retryAfterMillis(HttpResponse<String> response) {
        try {
            double seconds = JsonParser.parseString(response.body()).getAsJsonObject().get("retry_after").getAsDouble();
            return Math.max(250L, (long) (seconds * 1000));
        } catch (RuntimeException e) {
            return 2_000L;
        }
    }

    private static int parseColor(String raw) {
        try {
            return Integer.parseInt(raw.replace("#", "").trim(), 16);
        } catch (NumberFormatException e) {
            return 0x5865F2;
        }
    }

    private static String escapeMarkdown(String value) {
        return value.replaceAll("([_*~`|>\\\\])", "\\\\$1");
    }

    public void shutdown() {
        executor.shutdown();
        try {
            executor.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
