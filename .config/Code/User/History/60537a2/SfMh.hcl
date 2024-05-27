terraform {
  extra_arguments "vars" {
    commands = ["apply", "plan", "push", "refresh", "destroy"]
    arguments = [
      "-var-file=${get_terragrunt_dir()}/Sensitive.tg.tfvars",
      #"-var-file=${get_terragrunt_dir()}/../inventories/Manager/Manager.tfvars",
      "-auto-approve",
      ]
  }
  extra_arguments "destroy" {
    commands = ["destroy", "import"]
    arguments = [
      "-var-file=${get_terragrunt_dir()}/Sensitive.tg.tfvars", 
      #"-var-file=${get_terragrunt_dir()}/../inventories/Manager/Manager.tfvars",
      #"-input=false",
      ]
  }
}

generate "vars" {
  path      = "Sensitive.tg.tfvars"
  if_exists = "overwrite_terragrunt"
  contents = <<EOF
SPRING_LDAP_USERNAME = "${get_env("SPRING_LDAP_USERNAME")}"
SPRING_LDAP_ADMIN_PASSWORD           = "${get_env("SPRING_LDAP_ADMIN_PASSWORD")}"
EOF
}

generate "backend" {
  path      = "backend.tg.tf"
  if_exists = "overwrite_terragrunt"
  contents = <<EOF
# terraform {
#   backend "http" {
#     address        = "${get_env("TF_STATE_ADDRESS")}/terraform.tfstate"
##     lock_address   = "${get_env("TF_STATE_ADDRESS")}/terraform.tfstate.lock"
##     unlock_address = "${get_env("TF_STATE_ADDRESS")}/terraform.tfstate.lock"
#     username       = "${get_env("TF_USERNAME")}"
#     password       = "${get_env("TF_PASSWORD")}"
#     lock_method    = "POST" 
#     unlock_method  = "DELETE" 
#     retry_wait_min = "5"
#   }
# }
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