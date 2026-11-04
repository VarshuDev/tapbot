import * as fs from 'fs';
import * as path from 'path';
import * as zlib from 'zlib';
import * as crypto from 'crypto';

function createZipArchive(entries) {
    const localHeaders = [];
    const centralDirectoryHeaders = [];
    let offset = 0;

    for (const entry of entries) {
        const nameBuffer = Buffer.from(entry.name.replace(/\\/g, '/'), 'utf8');
        const uncompressedData = entry.data;
        const uncompressedSize = uncompressedData.length;
        const crc = zlib.crc32(uncompressedData);

        const compressedData = zlib.deflateRawSync(uncompressedData);
        const compressedSize = compressedData.length;

        const dosTime = (12 << 11) | (0 << 5) | 0;
        const dosDate = ((2026 - 1980) << 9) | (11 << 5) | 4;

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

const botDir = path.resolve('sample-bots/music-streamer-bot');
const manifestData = fs.readFileSync(path.join(botDir, 'manifest.json'));
const entrypointData = fs.readFileSync(path.join(botDir, 'entrypoint'));
const readmeData = fs.readFileSync(path.join(botDir, 'dependencies/README.txt'));
const iconData = fs.readFileSync(path.join(botDir, 'assets/icon.png'));

const entries = [
    { name: 'assets/icon.png', data: iconData },
    { name: 'dependencies/README.txt', data: readmeData },
    { name: 'entrypoint', data: entrypointData },
    { name: 'manifest.json', data: manifestData }
];

const zipBuffer = createZipArchive(entries);
const sha256 = crypto.createHash('sha256').update(zipBuffer).digest('hex');

const outDir = path.resolve('app/src/main/assets/packages');
const outPath = path.join(outDir, 'tapstream-music-1.0.0.botpkg');
fs.writeFileSync(outPath, zipBuffer);

// Also copy under package key alias tapstream-music_1.0.0.botpkg if needed
const aliasPath = path.join(outDir, 'tapstream-music_1.0.0.botpkg');
fs.writeFileSync(aliasPath, zipBuffer);

console.log(`Package generated: ${outPath}`);
console.log(`Size: ${zipBuffer.length} bytes`);
console.log(`SHA256: ${sha256}`);
