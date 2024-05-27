import imaplib

# Configuración
host = 'outlook.office365.com'
port = 993
email_address = 'BTuduri@geocom.com.uy'
access_token = 'TU_TOKEN_DE_ACCESO_OAUTH2'

# Conectar al servidor IMAP
mail = imaplib.IMAP4_SSL(host, port)

# Generar la cadena de autenticación para OAuth2
auth_string = 'user={}\1auth=Bearer {}\1\1'.format(email_address, access_token)

# Autenticarse utilizando OAuth2
mail.authenticate('XOAUTH2', lambda x: auth_string.encode())

# Seleccionar la bandeja de entrada y realizar operaciones...
mail.select('inbox')
