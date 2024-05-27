# Antonio Sarosi
# https://youtube.com/c/antoniosarosi
# https://github.com/antoniosarosi/dotfiles

# Qtile keybindings
from libqtile.config import Key
from libqtile.command import lazy


mod = "mod4"

keys = [Key(key[0], key[1], *key[2:]) for key in [
    # ------------ Window Configs ------------

    # Switch between windows in current stack pane
    ([mod], "Down", lazy.layout.down()),
    ([mod], "Up", lazy.layout.up()),
    ([mod], "Left", lazy.layout.left()),
    ([mod], "Right", lazy.layout.right()),

    # Change window sizes (MonadTall)
    ([mod, "shift"], "Right", lazy.layout.grow()),
    ([mod, "shift"], "Left", lazy.layout.shrink()),

    # Toggle floating
    ([mod, "shift"], "f", lazy.window.toggle_floating()),

    # Move windows up or down in current stack
    ([mod, "shift"], "Down", lazy.layout.shuffle_down()),
    ([mod, "shift"], "Up", lazy.layout.shuffle_up()),

    # Toggle between different layouts as defined below
    ([mod], "Tab", lazy.next_layout()),
    ([mod, "shift"], "Tab", lazy.prev_layout()),

    # Kill window
    ([mod], "w", lazy.window.kill()),

    # Switch focus of monitors
    ([mod], "period", lazy.next_screen()),
    ([mod], "comma", lazy.prev_screen()),

    # Restart Qtile
    ([mod, "control"], "r", lazy.restart()),

    ([mod, "control"], "q", lazy.shutdown()),
    ([mod], "c", lazy.spawncmd()),

    # ------------ App Configs ------------

    # Menu
    ([mod], "d", lazy.spawn("rofi -show drun")),

    # Window Nav
    ([mod, "shift"], "d", lazy.spawn("rofi -show")),

    # Browser
    ([mod], "b", lazy.spawn("firefox")),

    # File Explorer
    ([mod], "e", lazy.spawn("thunar")),

    # Terminal
    ([mod], "Return", lazy.spawn("alacritty")),

    # Redshift
    ([mod], "r", lazy.spawn("redshift -O 2400")),
    ([mod, "shift"], "r", lazy.spawn("redshift -x")),

    # Screenshot
    # # # full screen
    ([mod], "s", lazy.spawn("sh -c 'FILENAME=~/Pictures/screenshots/$(date \"+%Y-%m-%d_%H-%M-%S\").png; scrot $FILENAME && xclip -selection clipboard -t image/png -i $FILENAME'")),
    # # # selection
    ([mod, "shift"], "s", lazy.spawn("sh -c 'FILENAME=~/Pictures/screenshots/$(date \"+%Y-%m-%d_%H-%M-%S\")_select.png; scrot -s $FILENAME && xclip -selection clipboard -t image/png -i $FILENAME'")),
    # open vscode repos
    ([mod], "g", lazy.spawn("python3 /home/brian/pythonScripts/open_repos.py")),

    # keyboard layout
    ([mod], "space", lazy.spawn("/usr/bin/bash /home/brian/.config/qtile/scripts/dynamic_switch_keyboard.sh")),
    
    # lock screen
    ([mod], "l", lazy.spawn("/home/brian/.config/qtile/scripts/i3lock-solarized-dark.sh")),
    
    
    # ------------ Hardware Configs ------------

    # Volume
    ([], "XF86AudioLowerVolume", lazy.spawn(
        "pactl set-sink-volume @DEFAULT_SINK@ -5%"
    )),
    ([], "XF86AudioRaiseVolume", lazy.spawn(
        "pactl set-sink-volume @DEFAULT_SINK@ +5%"
    )),
    ([], "XF86AudioMute", lazy.spawn(
        "pactl set-sink-mute @DEFAULT_SINK@ toggle"
    )),

    # Brightness
    ([], "XF86MonBrightnessUp", lazy.spawn("brightnessctl set +10%")),
    ([], "XF86MonBrightnessDown", lazy.spawn("brightnessctl set 10%-")),
]]
