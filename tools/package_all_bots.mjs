import * as fs from 'fs';
import * as path from 'path';
import * as zlib from 'zlib';
import * as crypto from 'crypto';

function createZipArchive(entries) {
    const localHeaders = [];
    const centralDirectoryHeaders = [];
    let offset = 0;

    // Fixed DOS timestamp for reproducible byte-identical archives (2026-11-10 12:00:00)
    const dosTime = (12 << 11) | (0 << 5) | 0;
    const dosDate = ((2026 - 1980) << 9) | (11 << 5) | 10;

    for (const entry of entries) {
        const nameBuffer = Buffer.from(entry.name.replace(/\\/g, '/'), 'utf8');
        const uncompressedData = Buffer.isBuffer(entry.data)
            ? entry.data
            : Buffer.from(String(entry.data), 'utf8');
        const uncompressedSize = uncompressedData.length;
        const crc = zlib.crc32(uncompressedData);

        const compressedData = zlib.deflateRawSync(uncompressedData);
        const compressedSize = compressedData.length;

        const localHeader = Buffer.alloc(30);
        localHeader.writeUInt32LE(0x04034b50, 0);
        localHeader.writeUInt16LE(20, 4);
        localHeader.writeUInt16LE(0, 6);
        localHeader.writeUInt16LE(8, 8);
        localHeader.writeUInt16LE(dosTime, 10);
        localHeader.writeUInt16LE(dosDate, 12);
        localHeader.writeUInt32LE(crc, 14);
        localHeader.writeUInt32LE(compressedSize, 18);
        localHeader.writeUInt32LE(uncompressedSize, 22);
        localHeader.writeUInt16LE(nameBuffer.length, 26);
        localHeader.writeUInt16LE(0, 28);

        localHeaders.push(localHeader, nameBuffer, compressedData);

        const cdHeader = Buffer.alloc(46);
        cdHeader.writeUInt32LE(0x02014b50, 0);
        cdHeader.writeUInt16LE(20, 4);
        cdHeader.writeUInt16LE(20, 6);
        cdHeader.writeUInt16LE(0, 8);
        cdHeader.writeUInt16LE(8, 10);
        cdHeader.writeUInt16LE(dosTime, 12);
        cdHeader.writeUInt16LE(dosDate, 14);
        cdHeader.writeUInt32LE(crc, 16);
        cdHeader.writeUInt32LE(compressedSize, 20);
        cdHeader.writeUInt32LE(uncompressedSize, 24);
        cdHeader.writeUInt16LE(nameBuffer.length, 28);
        cdHeader.writeUInt16LE(0, 30);
        cdHeader.writeUInt16LE(0, 32);
        cdHeader.writeUInt16LE(0, 34);
        cdHeader.writeUInt16LE(0, 36);
        cdHeader.writeUInt32LE(0, 38);
        cdHeader.writeUInt32LE(offset, 42);

        centralDirectoryHeaders.push(cdHeader, nameBuffer);
        offset += localHeader.length + nameBuffer.length + compressedData.length;
    }

    const cdStartOffset = offset;
    const cdBuffer = Buffer.concat(centralDirectoryHeaders);
    const cdSize = cdBuffer.length;

    const eocd = Buffer.alloc(22);
    eocd.writeUInt32LE(0x06054b50, 0);
    eocd.writeUInt16LE(0, 4);
    eocd.writeUInt16LE(0, 6);
    eocd.writeUInt16LE(entries.length, 8);
    eocd.writeUInt16LE(entries.length, 10);
    eocd.writeUInt32LE(cdSize, 12);
    eocd.writeUInt32LE(cdStartOffset, 16);
    eocd.writeUInt16LE(0, 20);

    return Buffer.concat([...localHeaders, cdBuffer, eocd]);
}

const outDir = path.resolve('app/src/main/assets/packages');
fs.mkdirSync(outDir, { recursive: true });

// 1x1 transparent PNG reused from bundled sample icon so packages remain lightweight
const iconBuffer = fs.existsSync('sample-bots/commander-bot/assets/icon.png')
    ? fs.readFileSync('sample-bots/commander-bot/assets/icon.png')
    : Buffer.from(
        'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNkYAAAAAYAAjCB0C8AAAAASUVORK5CYII=',
        'base64'
    );

const specs = [
    {
        filename: 'bot_exon_1.0.0.botpkg',
        id: 'bot_1790084493920_n4l8lj',
        name: 'Exon Robot',
        version: '1.0.0',
        category: 'automation',
        runtime: 'native_art',
        entrypoint: 'com.tapbot.core.runner.runtime.ExonBotRuntime',
        description: 'Advanced Telegram group management bot with timed moderation, persisted rules, antiflood, locks, and inline menus.',
        permissions: ['INTERNET', 'FOREGROUND_SERVICE'],
        credentials: [
            { key: 'bot_token', label: 'Telegram Bot Token', required: true, secret: true, inputType: 'password' },
            { key: 'app_api_id', label: 'Telegram API ID (api_id)', required: true, secret: false, inputType: 'text' },
            { key: 'app_api_hash', label: 'Telegram API Hash (api_hash)', required: true, secret: true, inputType: 'password' },
            { key: 'owner_id', label: 'Bot Owner User ID', required: true, secret: false, inputType: 'text' }
        ]
    },
    {
        filename: 'bot_echo_1.0.0.botpkg',
        id: 'bot_echo',
        name: 'Echo Assistant Bot',
        version: '1.0.0',
        category: 'utilities',
        runtime: 'built-in',
        entrypoint: 'com.tapbot.core.runner.engine.EchoBotEngine',
        description: 'Lightweight Telegram bot that echoes messages with latency diagnostics and uptime telemetry.',
        permissions: ['INTERNET', 'FOREGROUND_SERVICE'],
        credentials: [
            { key: 'bot_token', label: 'Telegram Bot Token', required: true, secret: true, inputType: 'password' }
        ]
    },
    {
        filename: 'bot_rss_1.2.0.botpkg',
        id: 'bot_rss_notifier',
        name: 'RSS Channel Broadcaster',
        version: '1.2.0',
        category: 'media',
        runtime: 'built-in',
        entrypoint: 'com.tapbot.core.runner.runtime.RssNotifierBotRuntime',
        description: 'Monitors RSS 2.0 and Atom feeds with deduplication and broadcasts updates to Telegram channels.',
        permissions: ['INTERNET', 'FOREGROUND_SERVICE'],
        credentials: [
            { key: 'bot_token', label: 'Telegram Bot Token', required: true, secret: true, inputType: 'password' },
            { key: 'target_chat_id', label: 'Target Chat or Channel ID', required: true, secret: false, inputType: 'text' },
            { key: 'feed_url', label: 'RSS / Atom Feed URL', required: true, secret: false, inputType: 'text' },
            { key: 'poll_interval_minutes', label: 'Poll Interval (minutes)', required: false, secret: false, inputType: 'number' }
        ]
    },
    {
        filename: 'bot_ai_assistant_2.1.0.botpkg',
        id: 'bot_ai_assistant',
        name: 'Gemini Smart Assistant',
        version: '2.1.0',
        category: 'productivity',
        runtime: 'built-in',
        entrypoint: 'com.tapbot.core.runner.runtime.AIBotRuntime',
        description: 'Conversational AI assistant powered by Google Gemini with multi-turn memory, code mode, and summarization.',
        permissions: ['INTERNET', 'FOREGROUND_SERVICE'],
        credentials: [
            { key: 'bot_token', label: 'Telegram Bot Token', required: true, secret: true, inputType: 'password' },
            { key: 'gemini_api_key', label: 'Gemini API Key', required: true, secret: true, inputType: 'password' },
            { key: 'gemini_model', label: 'Gemini Model', required: false, secret: false, inputType: 'text' },
            { key: 'system_prompt', label: 'System Instruction', required: false, secret: false, inputType: 'text' }
        ]
    },
    {
        filename: 'bot_music_1.0.0.botpkg',
        id: 'bot_music',
        name: 'Music Bot',
        version: '1.0.0',
        category: 'media',
        runtime: 'native_art',
        entrypoint: 'com.tapbot.core.runner.runtime.MusicBotRuntime',
        description: 'Streams audio, queues playlists, and provides music playback controls on Android.',
        permissions: ['INTERNET', 'FOREGROUND_SERVICE'],
        credentials: [
            { key: 'bot_token', label: 'Telegram Bot Token', required: true, secret: true, inputType: 'password' },
            { key: 'spotify_client_id', label: 'Spotify Client ID', required: false, secret: false, inputType: 'text' }
        ]
    },
    {
        filename: 'bot_music_1.4.0.botpkg',
        id: 'bot_music',
        name: 'Music Bot',
        version: '1.4.0',
        category: 'media',
        runtime: 'native_art',
        entrypoint: 'com.tapbot.core.runner.runtime.MusicBotRuntime',
        description: 'High-resolution audio streaming, AudioFocus, /shuffle, /loop, /seek, and inline playback buttons.',
        permissions: ['INTERNET', 'FOREGROUND_SERVICE', 'MODIFY_AUDIO_SETTINGS', 'WAKE_LOCK'],
        credentials: [
            { key: 'bot_token', label: 'Telegram Bot Token', required: true, secret: true, inputType: 'password' },
            { key: 'spotify_client_id', label: 'Spotify Client ID', required: false, secret: false, inputType: 'text' }
        ]
    },
    {
        filename: 'bot_ai_1.0.0.botpkg',
        id: 'bot_ai',
        name: 'AI Bot',
        version: '1.0.0',
        category: 'productivity',
        runtime: 'native_art',
        entrypoint: 'com.tapbot.core.runner.runtime.AIBotRuntime',
        description: 'Generative AI conversational assistant powered by Google Gemini on-device.',
        permissions: ['INTERNET', 'FOREGROUND_SERVICE'],
        credentials: [
            { key: 'bot_token', label: 'Telegram Bot Token', required: true, secret: true, inputType: 'password' },
            { key: 'gemini_api_key', label: 'Gemini API Key', required: false, secret: true, inputType: 'password' },
            { key: 'gemini_model', label: 'Gemini Model', required: false, secret: false, inputType: 'text' }
        ]
    },
    {
        filename: 'bot_ai_1.2.0.botpkg',
        id: 'bot_ai',
        name: 'AI Bot',
        version: '1.2.0',
        category: 'productivity',
        runtime: 'native_art',
        entrypoint: 'com.tapbot.core.runner.runtime.AIBotRuntime',
        description: 'Multi-turn Gemini 2.5 assistant with /summarize, /code, /model, /system, and 4096-char auto-chunking.',
        permissions: ['INTERNET', 'FOREGROUND_SERVICE'],
        credentials: [
            { key: 'bot_token', label: 'Telegram Bot Token', required: true, secret: true, inputType: 'password' },
            { key: 'gemini_api_key', label: 'Gemini API Key', required: false, secret: true, inputType: 'password' },
            { key: 'gemini_model', label: 'Gemini Model', required: false, secret: false, inputType: 'text' }
        ]
    },
    {
        filename: 'bot_utility_1.0.0.botpkg',
        id: 'bot_utility',
        name: 'Utility Bot',
        version: '1.0.0',
        category: 'utilities',
        runtime: 'native_art',
        entrypoint: 'com.tapbot.core.runner.runtime.UtilityBotRuntime',
        description: 'System diagnostics, uptime monitoring, latency checks, and crash simulation on ART.',
        permissions: ['INTERNET', 'FOREGROUND_SERVICE'],
        credentials: [
            { key: 'bot_token', label: 'Telegram Bot Token', required: true, secret: true, inputType: 'password' }
        ]
    },
    {
        filename: 'bot_utility_1.1.0.botpkg',
        id: 'bot_utility',
        name: 'Utility Bot',
        version: '1.1.0',
        category: 'utilities',
        runtime: 'native_art',
        entrypoint: 'com.tapbot.core.runner.runtime.UtilityBotRuntime',
        description: 'Battery thermal reporting, memory diagnostics, and thread pool introspection.',
        permissions: ['INTERNET', 'FOREGROUND_SERVICE'],
        credentials: [
            { key: 'bot_token', label: 'Telegram Bot Token', required: true, secret: true, inputType: 'password' }
        ]
    },
    {
        filename: 'bot_ping_pong_1.0.0.botpkg',
        id: 'bot_ping_pong',
        name: 'Ping Pong Runner',
        version: '1.0.0',
        category: 'utilities',
        runtime: 'native_art',
        entrypoint: 'com.tapbot.core.runner.runtime.PingPongBotRuntime',
        description: 'Ultra-lightweight Telegram bot testing on-device latency and foreground execution.',
        permissions: ['INTERNET', 'FOREGROUND_SERVICE'],
        credentials: [
            { key: 'bot_token', label: 'Telegram Bot Token', required: true, secret: true, inputType: 'password' }
        ]
    }
];

const results = [];

for (const spec of specs) {
    const manifestObj = {
        $schema: 'https://tapbot.dev/schemas/v1/manifest.json',
        id: spec.id,
        name: spec.name,
        version: spec.version,
        description: spec.description,
        category: spec.category,
        icon: 'assets/icon.png',
        runtime: spec.runtime,
        entrypoint: spec.entrypoint,
        minimumRuntimeVersion: '1.0.0',
        minimumAppVersion: 1,
        credentials: spec.credentials,
        permissions: spec.permissions,
        packageMetadata: {
            author: 'TapBot v2.3.0',
            license: 'Apache-2.0',
            createdAt: '2026-11-10T00:00:00Z'
        }
    };

    const manifestJson = JSON.stringify(manifestObj, null, 2) + '\n';
    const entrypointText = `${spec.entrypoint}\n`;
    const readmeText = `${spec.name} v${spec.version}\nRuntime: ${spec.runtime}\nEntrypoint: ${spec.entrypoint}\n`;

    const entries = [
        { name: 'assets/icon.png', data: iconBuffer },
        { name: 'dependencies/README.txt', data: readmeText },
        { name: 'entrypoint', data: entrypointText },
        { name: 'manifest.json', data: manifestJson }
    ];

    const zipBuffer = createZipArchive(entries);
    const outPath = path.join(outDir, spec.filename);
    fs.writeFileSync(outPath, zipBuffer);

    const sha256 = crypto.createHash('sha256').update(zipBuffer).digest('hex');
    results.push({
        filename: spec.filename,
        id: spec.id,
        version: spec.version,
        size: zipBuffer.length,
        sha256
    });
}

// Also verify flagship archives remain intact & report their hashes
const flagships = ['tapbot-commander_1.0.0.botpkg', 'tapstream-music-1.0.0.botpkg'];
for (const f of flagships) {
    const full = path.join(outDir, f);
    if (fs.existsSync(full)) {
        const buf = fs.readFileSync(full);
        results.push({
            filename: f,
            id: f.includes('commander') ? 'bot_commander' : 'bot_music_streamer',
            version: '1.0.0',
            size: buf.length,
            sha256: crypto.createHash('sha256').update(buf).digest('hex'),
            preserved: true
        });
    }
}

console.log(JSON.stringify(results, null, 2));
