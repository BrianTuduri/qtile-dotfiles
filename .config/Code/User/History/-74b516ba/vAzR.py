from libqtile import widget
from .theme import colors
from libqtile.lazy import lazy
import subprocess
# Get the icons at https://www.nerdfonts.com/cheat-sheet (you need a Nerd Font)

# set interface name
cmdGetInterfaceName = "nmcli -t -f TYPE,DEVICE connection show --active | sort | grep -E 'ethernet|wifi' | head -n 1 | cut -d':' -f2"
interfaceName = subprocess.run(cmdGetInterfaceName, shell=True, capture_output=True, text=True).stdout.strip().lower()
interfaceName = interfaceName if interfaceName else "wlp3s0"

def base(fg='text', bg='dark'): 
    return {
        'foreground': colors[fg],
        'background': colors[bg]
    }


def separator():
    return widget.Sep(**base(), linewidth=0, padding=5)

def icon(fg='text', bg='dark', fontsize=16, text="?"):
    return widget.TextBox(
        **base(fg, bg),
        fontsize=fontsize,
        text=text,
        padding=3
    )

def powerline(fg="light", bg="dark"):
    return widget.TextBox(
        **base(fg, bg),
        text="", # Icon: nf-oct-triangle_left
        fontsize=37,
        padding=-2
    )

def workspaces():
    return [
        separator(),
        widget.GroupBox(
            **base(fg='light'),
            font='UbuntuMono Nerd Font',
            fontsize=19,
            margin_y=3,
            margin_x=0,
            padding_y=8,
            padding_x=5,
            borderwidth=1,
            active=colors['active'],
            inactive=colors['inactive'],
            rounded=False,
            highlight_method='block',
            urgent_alert_method='block',
            urgent_border=colors['urgent'],
            this_current_screen_border=colors['focus'],
            this_screen_border=colors['grey'],
            other_current_screen_border=colors['dark'],
            other_screen_border=colors['dark'],
            disable_drag=True
        ),
        separator(),
        widget.WindowName(**base(fg='focus'), fontsize=14, padding=5),
        separator(),
    ]

primary_widgets = [
    *workspaces(),

    separator(),
    
    icon(text=" "),  # Icono de portapapeles (nf-mdi-content_paste)
    
    widget.Clipboard(
        selection="CLIPBOARD",  # O "PRIMARY" si deseas mostrar la selección primaria
        background=colors['dark'],
        max_width=15,            # Número máximo de caracteres a mostrar
        timeout=10,              # Tiempo en segundos para limpiar el texto, None para mantenerlo siempre
        blacklist=["keepassx"],  # Lista de clases WM en la lista negra
        blacklist_text="***********"  # Texto a mostrar cuando la clase WM está en la lista negra
    ),
    
    widget.TextBox(
        text=' ',  # Icono de portapapeles (nf-fa-clipboard)
        background=colors['dark'],
        font='FiraCode Nerd Font Mono',
        fontsize=16,
        padding=3,
        mouse_callbacks={'Button1': lazy.spawn('xfce4-clipman-history')},  # Acción al hacer clic
    ),

    powerline('color6', 'dark'),

    icon(bg="color6", text=' '), # Icon: nf-fa-download

    widget.CheckUpdates(
        background=colors['color6'],
        colour_have_updates=colors['text'],
        colour_no_updates=colors['text'],
        no_update_string='0',
        display_format='{updates}',
        update_interval=1800,
        custom_command='checkupdates',
    ),

    powerline('color3', 'color6'),

    icon(bg="color3", text=' '),  # Icon: nf-fa-feed

    widget.Net(
        **base(bg='color3'),
        interface=interfaceName,
        format=f"{interfaceName}:" + ' {down} ↓↑ {up}',
        padding=5,
        update_interval=1,
    ),

    powerline('color2', 'color3'),

    widget.GenPollText(
        update_interval=5, # seconds
        func=lambda: subprocess.check_output("setxkbmap -query | awk '/layout:/ {print $2}'", shell=True).decode('utf-8').strip(),
        **base(bg='color2'),
        padding=5,
    ),
    powerline('color5', 'color2'), # types layout color

    widget.CurrentLayoutIcon(**base(bg='color5'), scale=0.65),

    widget.CurrentLayout(**base(bg='color5'), padding=5),

    powerline('dark', 'color5'),

    widget.Clock(**base(bg='dark'), format='%d/%m/%Y - %H:%M '),

    powerline('dark', 'dark'),

    widget.WidgetBox(
        widgets=[
            widget.Systray(background=colors['dark'], padding=5),
        ],
        background="#282c34",
        close_button_location='right',
        font='sans',
        fontsize=12,
        foreground='ffffff',
        text_closed='[<]',
        text_open='[>]',
        start_opened=False,
        padding=5
    ),
]

secondary_widgets = [
    *workspaces(),

    separator(),

    powerline('color1', 'dark'),

    widget.CurrentLayoutIcon(**base(bg='color1'), scale=0.65),

    widget.CurrentLayout(**base(bg='color1'), padding=5),

    powerline('color2', 'color1'),
    #powerline('dark', 'color2'),
]

widget_defaults = {
    'font': 'UbuntuMono Nerd Font Bold',
    'fontsize': 14,
    'padding': 1,
}
extension_defaults = widget_defaults.copy()
