import subprocess
import os

# Leer las variables de entorno
REPO_PATH = os.getenv('REPO_PATH', '/path/to/repos')
SELECTOR = os.getenv('SELECTOR', 'rofi')  # Puede ser 'wofi' o 'rofi'
EDITOR = os.getenv('EDITOR', 'code')  # Puede ser 'code', 'codium', 'intellij', etc.

try:
    # Comando para listar solo los nombres de los directorios en el path especificado
    command = f"find {REPO_PATH} -maxdepth 1 -type d -exec basename {{}} \;"
    repos = subprocess.check_output(command, shell=True, text=True).strip().split('\n')
except subprocess.CalledProcessError as e:
    print(f"Error al listar los repositorios: {e}")
    exit(1)


# Preparar la lista de repositorios como entrada para wofi/rofi
input_str = "\n".join(repos)

# Ejecutar wofi/rofi y capturar la elección
try:
    selected_repo = subprocess.run([SELECTOR, '-dmenu'], input=input_str, text=True, capture_output=True, check=True).stdout.strip()
except subprocess.CalledProcessError as e:
    print(f"Error al seleccionar el repositorio: {e}")
    exit(1)

# Abrir el repositorio seleccionado con el editor especificado
if selected_repo:
    try:
        subprocess.Popen([EDITOR, f"{REPO_PATH}/{selected_repo}"])
    except Exception as e:
        print(f"Error al abrir el repositorio con el editor: {e}")
else:
    print("No se seleccionó ningún repositorio.")

