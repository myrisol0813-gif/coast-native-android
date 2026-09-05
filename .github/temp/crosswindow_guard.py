from pathlib import Path

p = Path('.github/workflows/android-debug.yml')
text = p.read_text()
replacements = [
    ('          test -f "$remote/RemoteDogtalk.kt"\n', '          test -f "$remote/RemoteDogtalk.kt"\n          test -f "$remote/RemoteCrossWindow.kt"\n'),
    ('          test -f "$model/ModelDisplayName.kt"\n', '          test -f "$model/ModelDisplayName.kt"\n          test -f "$model/CrossWindowModels.kt"\n'),
    ('          test -f "$dogtalk/DogtalkUiState.kt"\n', '          test -f "$dogtalk/DogtalkUiState.kt"\n          test -f "$dogtalk/CrossWindowRepository.kt"\n          test -f "$dogtalk/CrossWindowUiState.kt"\n'),
    ('          grep -q \'DefaultDogtalkRepository\' "$shell/CoastBackendGraph.kt"\n', '          grep -q \'DefaultDogtalkRepository\' "$shell/CoastBackendGraph.kt"\n          grep -q \'/api/chat/cross-window/sources\' "$dogtalk/CrossWindowRepository.kt"\n          grep -q \'DefaultCrossWindowRepository\' "$shell/CoastBackendGraph.kt"\n'),
    ('          # Context-label and turn-desk contract: nine source sections, collapsible Native cards, no flat dump owner.\n', '          # Context-label and turn-desk contract: cross-window is a first-class tenth source section.\n'),
    ('          grep -q \'ifBlank { "神秘狗话" }\' "$chat/TurnDeskMapper.kt"\n', '          grep -q \'ifBlank { "狗话" }\' "$chat/TurnDeskMapper.kt"\n          grep -q \'ifBlank { "跨窗口取信" }\' "$chat/TurnDeskMapper.kt"\n'),
]
for old, new in replacements:
    count = text.count(old)
    if count != 1:
        raise SystemExit(f'guard anchor count {count}: {old!r}')
    text = text.replace(old, new, 1)
p.write_text(text)
