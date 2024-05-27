
import msal
import webbrowser
from http.server import HTTPServer, BaseHTTPRequestHandler
from urllib.parse import urlparse, parse_qs

# Configuración de la aplicación
client_id = 'e71d7f86-ad3b-44e2-9d21-07d70dac49c8'
authority = 'https://login.microsoftonline.com/1bb99a7e-c900-4b1a-8adf-261d8c3c5501'
client_secret = 'e2efd0e2-4807-4796-9d2a-4a375b5669cc'
redirect_uri = 'http://localhost:8000'
scope = ['https://outlook.office.com/IMAP.AccessAsUser.All']

# Configura la instancia de MSAL
app = msal.ConfidentialClientApplication(client_id, authority=authority,
                                         client_credential=client_secret)

# Genera la URL de autenticación y abre en el navegador
auth_url = app.get_authorization_request_url(scope, redirect_uri=redirect_uri)
webbrowser.open(auth_url)

# Servidor local para interceptar el código de redirección
class RedirectHandler(BaseHTTPRequestHandler):
    def do_GET(self):
        self.send_response(200)
        self.send_header('Content-type', 'text/html')
        self.end_headers()
        query = urlparse(self.path).query
        code = parse_qs(query)['code'][0]
        self.wfile.write(f'Código de autorización: {code}'.encode())

        # Usa el código para obtener el token de acceso
        result = app.acquire_token_by_authorization_code(code, scopes=scope, redirect_uri=redirect_uri)
        print(result)

httpd = HTTPServer(('localhost', 8000), RedirectHandler)
httpd.handle_request()
