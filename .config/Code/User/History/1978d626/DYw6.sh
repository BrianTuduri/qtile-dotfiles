#!/bin/bash
set -xe

layouts=("us" "es")
current_layout=$(setxkbmap -query | awk '/layout:/ {print $2}')

current_index=-1
for i in "${!layouts[@]}"; do
    if [[ "${layouts[$i]}" = "${current_layout}" ]]; then
        current_index=$i
        break
    fi
done

if [[ $current_index -ne -1 ]]; then
    next_index=$(( (current_index + 1) % ${#layouts[@]} ))
    next_layout=${layouts[$next_index]}
    setxkbmap $next_layout
else
    echo "Layout actual no encontrado en la lista de layouts definida."
fi
