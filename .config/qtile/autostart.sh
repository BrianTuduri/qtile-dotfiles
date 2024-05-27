#!/bin/sh

# systray battery icon
cbatticon -u 5 &
# systray volume
volumeicon &

# set wallpaper
feh --bg-fill $HOME/Pictures/wallpaper.jpg

# run picom
picom &

# nm-applet
nm-applet &

# clipman
xfce4-clipman &

# flameshot
flatpak run org.flameshot.Flameshot &

