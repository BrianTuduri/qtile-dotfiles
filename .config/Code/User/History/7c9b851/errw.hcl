pid_file = "{{ vault_agent_dir }}/pidfile"

vault {
    address = "{{ vault_addr }}"
    retry {
        num_retries = {{ retry_num_retries }}
    }
}

auto_auth {
    {% if auth_method == "approle" %}
    method "approle" {
        config = {
            role_id_file_path = "{{ vault_agent_dir }}/{{ role_id_file }}"
            secret_id_file_path = "{{ vault_agent_dir }}/{{ secret_id_file }}"
            remove_secret_id_file_after_reading = {{ remove_secret_id_file_after_reading }}
        }
    }
    {% elif auth_method == "aws" %}
    method "aws" {
        mount_path = "{{ aws_mount_path }}"
        config = {
            type = "iam"
            role = "{{ aws_role }}"
        }
    }
    {% elif auth_method == "userpass" %}
    method "userpass" {
        config = {
            username = "{{ userpass_username }}"
            password = "{{ userpass_password }}"
        }
    }
    {% endif %}

    sink "file" {
        config = {
            path = "{{ vault_agent_dir }}/{{ token_file }}"
        }
    }

    {% if additional_sinks %}
    {% for sink in additional_sinks %}
    sink "{{ sink.type }}" {
        {% if sink.wrap_ttl %}
        wrap_ttl = "{{ sink.wrap_ttl }}"
        {% endif %}
        {% if sink.aad_env_var %}
        aad_env_var = "{{ sink.aad_env_var }}"
        {% endif %}
        {% if sink.dh_type %}
        dh_type = "{{ sink.dh_type }}"
        {% endif %}
        {% if sink.dh_path %}
        dh_path = "{{ sink.dh_path }}"
        {% endif %}
        config = {
            path = "{{ sink.path }}"
        }
    }
    {% endfor %}
    {% endif %}
}

cache {
    use_auto_auth_token = {{ use_auto_auth_token }}
}

api_proxy {
    use_auto_auth_token = {{ use_auto_auth_token }}
}

listener "unix" {
    address = "{{ unix_listener_address }}"
    tls_disable = {{ tls_disable }}

    agent_api {
        enable_quit = {{ enable_quit }}
    }
}

listener "tcp" {
    address = "{{ tcp_listener_address }}"
    tls_disable = {{ tls_disable }}
}

{% if templates %}
{% for template in templates %}
template {
    source = "{{ template.source }}"
    destination = "{{ template.destination }}"
}
{% endfor %}
{% endif %}
