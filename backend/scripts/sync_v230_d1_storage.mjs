/**
 * Sync all v2.3.0 bundled .botpkg archives from app/src/main/assets/packages/
 * into Cloudflare D1 `storage_objects` table (zero-R2 free tier deployment).
 *
 * Usage:
 *   node scripts/sync_v230_d1_storage.mjs --local
 *   node scripts/sync_v230_d1_storage.mjs
 */

import * as fs from 'fs';
import * as os from 'os';
import * as path from 'path';
import * as crypto from 'crypto';
import { execFileSync } from 'child_process';

const args = new Set(process.argv.slice(2));
const isLocal = args.has('--local');

const assetsPackagesDir = path.resolve('..', 'app', 'src', 'main', 'assets', 'packages');
const BOTPKG_FILES = [
    'bot_ai_1.0.0.botpkg',
    'bot_ai_1.2.0.botpkg',
    'bot_ai_assistant_2.1.0.botpkg',
    'bot_echo_1.0.0.botpkg',
    'bot_exon_1.0.0.botpkg',
    'bot_music_1.0.0.botpkg',
    'bot_music_1.4.0.botpkg',
    'bot_ping_pong_1.0.0.botpkg',
    'bot_rss_1.2.0.botpkg',
    'bot_utility_1.0.0.botpkg',
    'bot_utility_1.1.0.botpkg',
    'tapbot-commander_1.0.0.botpkg',
    'tapstream-music-1.0.0.botpkg'
];

const now = new Date().toISOString();
const statements = [];

for (const filename of BOTPKG_FILES) {
    const filePath = path.join(assetsPackagesDir, filename);
    if (!fs.existsSync(filePath)) {
        throw new Error(`Missing bundled package archive: ${filePath}`);
    }

    const bytes = fs.readFileSync(filePath);
    const sha256 = crypto.createHash('sha256').update(bytes).digest('hex');
    const base64 = bytes.toString('base64');
    const etag = `"${sha256.slice(0, 32)}"`;
    const key = `packages/${filename}`;
    const safeKey = key.replace(/'/g, "''");
    const safeEtag = etag.replace(/'/g, "''");

    console.log(`[D1-Storage] Staging ${key} (${bytes.length} bytes, sha256=${sha256.slice(0, 12)}...)`);
    statements.push(
        `INSERT INTO storage_objects (key, content_type, size, data_base64, etag, created_at) ` +
        `VALUES ('${safeKey}', 'application/octet-stream', ${bytes.length}, '${base64}', '${safeEtag}', '${now}') ` +
        `ON CONFLICT(key) DO UPDATE SET content_type = excluded.content_type, size = excluded.size, data_base64 = excluded.data_base64, etag = excluded.etag, created_at = excluded.created_at;`
    );
}

const tmpSqlPath = path.join(os.tmpdir(), `tapbot_d1_storage_${Date.now()}.sql`);
fs.writeFileSync(tmpSqlPath, statements.join('\n'), 'utf8');

try {
    const wranglerArgs = [
        'wrangler',
        'd1',
        'execute',
        'DB',
        isLocal ? '--local' : '--remote',
        '--file',
        tmpSqlPath
    ];

    if (process.platform === 'win32') {
        execFileSync('cmd.exe', ['/d', '/s', '/c', 'npx', ...wranglerArgs], { stdio: 'inherit' });
    } else {
        execFileSync('npx', wranglerArgs, { stdio: 'inherit' });
    }

    console.log(`\nDone: synced ${BOTPKG_FILES.length} packages into D1 storage_objects (${isLocal ? 'local' : 'remote'}).`);
} finally {
    if (fs.existsSync(tmpSqlPath)) {
        fs.unlinkSync(tmpSqlPath);
    }
}
