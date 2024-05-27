import imaplib
import email
from email.header import decode_header
import webbrowser
import os
import re

# Configuración de conexión
imap_url = 'outlook.office365.com'
username = 'BTuduri'
password = 'Alex271103$.$'

# Conectarse al servidor IMAP y seleccionar la bandeja de entrada
mail = imaplib.IMAP4_SSL(imap_url)
mail.login(username, password)
mail.select('inbox')

# Buscar correos electrónicos no leídos
status, messages = mail.search(None, 'UNSEEN')
if status == 'OK':
    for num in messages[0].split():
        typ, data = mail.fetch(num, '(RFC822)')
        for response_part in data:
            if isinstance(response_part, tuple):
                # Parsear el cuerpo del correo
                msg = email.message_from_bytes(response_part[1])
                email_subject = decode_header(msg['subject'])[0][0]
                email_from = msg['from']
                email_to = msg['to']

                print(f"De: {email_from}")
                print(f"Para: {email_to}")
                print(f"Asunto: {email_subject}\n")

                # Extraer cuerpo del mensaje
                if msg.is_multipart():
                    for part in msg.walk():
                        if part.get_content_type() == "text/plain":
                            body = part.get_payload(decode=True).decode()
                            print("Cuerpo del mensaje:", body)
                else:
                    body = msg.get_payload(decode=True).decode()
                    print("Cuerpo del mensaje:", body)

                # Extraer URLs de GitLab, nombres de proyectos y usuarios
                gitlab_urls = re.findall(r'https://gitlab.[\w\.-]+/[\w\.-]+', body)
                print("URLs de GitLab encontradas:", gitlab_urls)

                # Extraer nombres de proyecto basados en una suposición simplificada de formato
                nombres_proyectos = re.findall(r'\b[\w-]+$', body, re.MULTILINE)
                print("Nombres de proyectos:", nombres_proyectos)

                # En este punto, agregar lógica para extraer usuarios específicamente mencionados si se desea

# Cerrar la conexión
mail.logout()
