import imaplib

# Configuración
host = 'outlook.office365.com'
port = 993
email_address = 'BTuduri@geocom.com.uy'
access_token = '0.AQ8Afpq5GwDJGkuK3yYdjDxVAYZ_Hec7reJEnSEH1w2sScgPADI.AgABAAIAAADnfolhJpSnRYB1SVj-Hgd8AgDs_wUA9P9BdDfmKhlvU0EDt0v9eGogN0K6xzDl5hl0iRY6JyYFgdaQe9KDiv2xONAfHdylCbke1lzwcVlGnsazDwxj7rUbBbBynG7YoaoAQqPsrwPypMLhchE51UTYkHQCK8PJVRNl5spOyQ4M14x9LPeWunOp1CdwsBev6SmF-97nCgLzNiRfZ_IpRsK20I2Xn4QUYzPhJ2tzQ-abdQFIDEaKGBr6vLnmBKv4haKTvRUQeLjG7UA57DbheRVLBMz6xubPBt3cVHsd1a0LTCE8rJtlwVs4OptxGnyHh6XXWfPL8vQkhqD-nafalB4tZxOtSfwmvs5Ysa-8fLTjFKtrgc11KYxEFyOey3znHLgf2CwTdAb48lBQDQvkObFa-Rw0EgadBBTN-B98T3-qqtN8gPaiZuUq7xb8sQRthTU5b42FzzmTDchCnNt4y-Po14jB6Cp3RdiWXOrWBUv_uGrcH7yWI6XZlxNkcU4d1umb8aY2r3MlSZzp0O_EK9Gr8Dfd6UHGR2_uK2PLnj0cigRtcz8nxVlAUsWNdo4QmKH7eHkRK1DCG7j9hrwS-D4Yk5daEydNes79_fTUd4gbTkCj2hWWxGwkTgd6_dPPwEUFKu1KLtvU5_aOeG8SBxdzi6Tz_WqbPzHHHgioGhE9WBpB3kLY5edR6a8flts4dDuMynneVaeTfwnorYwG5IZX_YTYD486RpgBsEa_1EXPyQuMUzsczZZNt1KXhf8iP-bua-osyD4G-fH4r6ZU0YX_2eJrKy2bRw05kr7jIxMZCAb3qVBstiA9t1z4Sc4TzJhhVDu_OEaf9g'

# Conectar al servidor IMAP
mail = imaplib.IMAP4_SSL(host, port)

# Generar la cadena de autenticación para OAuth2
auth_string = 'user={}\1auth=Bearer {}\1\1'.format(email_address, access_token)

# Autenticarse utilizando OAuth2
mail.authenticate('XOAUTH2', lambda x: auth_string.encode())

# Seleccionar la bandeja de entrada y realizar operaciones...
mail.select('inbox')