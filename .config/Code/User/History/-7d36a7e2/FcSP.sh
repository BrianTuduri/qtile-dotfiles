#!/bin/sh

# systray battery icon
cbatticon -u 5 &
# systray volume
volumeicon &

# set wallpaper
feh --bg-fill /hom/brian/Pictures/wallpaper.jpg

# run picom
picom &