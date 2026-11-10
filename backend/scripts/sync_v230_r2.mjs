/**
 * Upload all v2.3.0 bundled .botpkg archives from app/src/main/assets/packages/
 * into Cloudflare R2 bucket `tapbot-packages` under `packages/<filename>`.
 *
 * Usage:
 *   node scripts/sync_v230_r2.mjs --local   # seed local Wrangler R2 emulator
 *   node scripts/sync_v230_r2.mjs           # upload to remote Cloudflare R2
 */

import * as fs from 'fs';
import * as path from 'path';
import * as crypto from 'crypto';
import { execFileSync } from 'child_process';

const args = new Set(process.argv.slice(2));
const isLocal = args.has('--local');
const bucketName = process.env.R2_BUCKET_NAME || 'tapbot-packages';

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

const npxCmd = 'npx';

for (const filename of BOTPKG_FILES) {
    const filePath = path.join(assetsPackagesDir, filename);
    if (!fs.existsSync(filePath)) {
        throw new Error(`Missing bundled package archive: ${filePath}`);
    }

    const bytes = fs.readFileSync(filePath);
    const sha256 = crypto.createHash('sha256').update(bytes).digest('hex');
    const r2Path = `${bucketName}/packages/${filename}`;

    const wranglerArgs = [
        'wrangler',
        'r2',
        'object',
        'put',
        r2Path,
        '--file',
        filePath,
        '--content-type',
        'application/octet-stream'
    ];
    if (isLocal) {
        wranglerArgs.push('--local');
    }

    console.log(`[R2] Uploading ${r2Path} (${bytes.length} bytes, sha256=${sha256.slice(0, 12)}...) [${isLocal ? 'local' : 'remote'}]`);
    if (process.platform === 'win32') {
        execFileSync('cmd.exe', ['/d', '/s', '/c', npxCmd, ...wranglerArgs], {
            stdio: 'inherit'
        });
    } else {
        execFileSync(npxCmd, wranglerArgs, {
            stdio: 'inherit'
        });
    }
}

console.log(`\nDone: synced ${BOTPKG_FILES.length} v2.3.0 .botpkg archives to ${bucketName} (${isLocal ? 'local' : 'remote'}).`);
