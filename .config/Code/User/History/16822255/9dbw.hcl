pid_file = "/home/brian/vault-agent-docker/pidfile"

vault {
    address = "https://vault.geocom.com.uy"
    retry {
        num_retries = 5
    }
}

auto_auth {
    method {
        type = "approle"

        config = {
            role_id_file_path = "/home/brian/vault-agent-docker/geoswitch-role_id"
            secret_id_file_path = "/home/brian/vault-agent-docker/geoswitch-secret_id"
            remove_secret_id_file_after_reading = false
        }
    }

    sink {
        type = "file"
        config = {
            path = "/home/brian/vault-agent-docker/token"
        }
    }

}

cache {
    use_auto_auth_token = true
}




listener "tcp" {
    address = "127.0.0.1:8100"
    tls_disable = true
}

