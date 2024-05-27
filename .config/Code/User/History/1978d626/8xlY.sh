#!/bin/bash
set -xe
current=$(swaymsg -t get_inputs | jq -r '.[] | select(.type=="keyboard") | .xkb_active_layout_name')
layouts=("English (US)" "Spanish")
current_index=-1
for i in "${!layouts[@]}"; do
    if [[ "${layouts[$i]}" = "${current}" ]]; then
        current_index=$i
        break
    fi
done
if [[ $current_index -ne -1 ]]; then
    next_index=$(( (current_index + 1) % ${#layouts[@]} ))
    next_layout=${layouts[$next_index]}
    swaymsg input type:keyboard xkb_layout "${next_layout}"
fi
