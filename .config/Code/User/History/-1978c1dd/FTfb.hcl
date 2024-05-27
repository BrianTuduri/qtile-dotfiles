terraform {
  extra_arguments "vars" {
    commands = ["apply", "plan", "import", "push", "refresh", "destroy"]
    arguments = [
      "-lock=false",
      "-auto-approve",
      ]
  }
}

locals {
  rancher_url  = "https://rancher.geocom.com.uy/v3"
  rancher_user  = "token-dtd9v"
  rancher_password  = "5sp4fj4cpm99f979k6zzm8fn9rm9mnwqmn2fwtqjbfbcdpksfwvg5w"
}

generate "backend" {
  path      = "backend.tg.tf"
  if_exists = "overwrite_terragrunt"
  contents = <<EOF
terraform {
  backend "http" {
    address        = "${get_env("TF_STATE_ADDRESS")}/terraform.tfstate"
    #lock_address   = "${get_env("TF_STATE_ADDRESS")}/terraform.tfstate.lock"
    #unlock_address = "${get_env("TF_STATE_ADDRESS")}/terraform.tfstate.lock"
    username       = "${get_env("TF_USERNAME")}"
    password       = "${get_env("TF_PASSWORD")}"
    lock_method    = "POST" 
    unlock_method  = "DELETE" 
    retry_wait_min = "5"
  }
  required_providers {
    rancher2 = {
      source = "rancher/rancher2"
    }
  }
}

provider "rancher2" {
  api_url    = "${local.rancher_url}"
  access_key = "${local.rancher_user}"
  secret_key = "${local.rancher_password}"
}
provider "helm" {
  kubernetes {
    config_path = "${get_env("KUBECONFIG_PATH")}"
  }
}
provider "kubernetes" {
  config_path = "${get_env("KUBECONFIG_PATH")}"
}
EOF
}