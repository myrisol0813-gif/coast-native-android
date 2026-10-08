#!/usr/bin/env python3
"""Check that the actual bundled NanoOldSong TTF maps common Chinese to glyphs.

This uses only Python's standard library and reads OpenType cmap format 4/12.
It prevents accidentally bundling an extension-only NanoOldSong shard.
"""
import mmap
import struct
import sys


SAMPLE = "小寒你好我们在海岸写信聊天文字与回忆都留在这里"


def u16(buf, pos):
    return struct.unpack_from(">H", buf, pos)[0]


def u32(buf, pos):
    return struct.unpack_from(">I", buf, pos)[0]


def format4_maps(buf, base, codepoint):
    if codepoint > 0xFFFF:
        return False
    segments = u16(buf, base + 6) // 2
    end_base = base + 14
    start_base = end_base + segments * 2 + 2
    delta_base = start_base + segments * 2
    range_base = delta_base + segments * 2
    for i in range(segments):
        end = u16(buf, end_base + i * 2)
        if codepoint > end:
            continue
        start = u16(buf, start_base + i * 2)
        if codepoint < start:
            return False
        delta = u16(buf, delta_base + i * 2)
        range_offset = u16(buf, range_base + i * 2)
        if range_offset == 0:
            return (codepoint + delta) & 0xFFFF != 0
        glyph_pos = range_base + i * 2 + range_offset + 2 * (codepoint - start)
        glyph = u16(buf, glyph_pos)
        return glyph != 0 and (glyph + delta) & 0xFFFF != 0
    return False


def format12_maps(buf, base, codepoint):
    count = u32(buf, base + 12)
    lo, hi = 0, count - 1
    while lo <= hi:
        mid = (lo + hi) // 2
        offset = base + 16 + mid * 12
        start, end, glyph = struct.unpack_from(">III", buf, offset)
        if codepoint < start:
            hi = mid - 1
        elif codepoint > end:
            lo = mid + 1
        else:
            return glyph + codepoint - start != 0
    return False


def check_font(path):
    with open(path, "rb") as file:
        with mmap.mmap(file.fileno(), 0, access=mmap.ACCESS_READ) as buf:
            num_tables = u16(buf, 4)
            cmap = None
            for i in range(num_tables):
                tag, _, offset, length = struct.unpack_from(">4sIII", buf, 12 + i * 16)
                if tag == b"cmap":
                    cmap = (offset, length)
                    break
            if cmap is None:
                raise ValueError("Font has no cmap table")
            cmap_offset, cmap_length = cmap
            subtables = []
            for i in range(u16(buf, cmap_offset + 2)):
                platform, encoding, relative = struct.unpack_from(">HHI", buf, cmap_offset + 4 + i * 8)
                if platform not in (0, 3) or relative >= cmap_length:
                    continue
                base = cmap_offset + relative
                kind = u16(buf, base)
                if kind in (4, 12):
                    subtables.append((kind, base))
            if not subtables:
                raise ValueError("Font has no supported Unicode cmap format 4/12")
            missing = []
            for char in SAMPLE:
                cp = ord(char)
                if not any(
                    format4_maps(buf, offset, cp) if kind == 4
                    else format12_maps(buf, offset, cp)
                    for kind, offset in subtables
                ):
                    missing.append(char)
            if missing:
                raise ValueError("Missing common Chinese glyphs: " + "".join(missing))
            print(f"NanoOldSong glyph check PASS: {len(SAMPLE)} common Chinese characters map to nonzero glyphs")


if __name__ == "__main__":
    if len(sys.argv) != 2:
        raise SystemExit("Usage: verify_nano_old_song.py <NanoOldSongA-Regular.ttf>")
    try:
        check_font(sys.argv[1])
    except (OSError, ValueError, struct.error) as error:
        raise SystemExit(f"NanoOldSong glyph check FAILED: {error}")
