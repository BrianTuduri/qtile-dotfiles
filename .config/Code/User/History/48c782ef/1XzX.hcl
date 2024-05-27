pid_file = "./pidfile"

auto_auth {
  method {
    type = "approle"

    config = {
      role_id_file_path = "/vault-agent/geoswitch-role_id"
      secret_id_file_path = "/vault-agent/geoswitch-secret_id"
      remove_secret_id_file_after_reading = false
    }
  }

  sink {
    type = "file"
    config = {
      path = "/vault-agent/token"
    }
  }
}

cache {
  use_auto_auth_token = true
}

listener "tcp" {
    address = "0.0.0.0:8200"
    tls_disable = true
}

vault {
  address = "https://vault.geocom.com.uy"
}
