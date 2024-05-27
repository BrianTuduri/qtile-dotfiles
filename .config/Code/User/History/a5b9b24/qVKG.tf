terraform {
  backend "http" {
    address        = "https://gitlab.geocom.com.uy/api/v4/projects/2951/terraform/state/vault-qa"
    lock_address   = "https://gitlab.geocom.com.uy/api/v4/projects/2951/terraform/state/vault-qa/lock"
    unlock_address = "https://gitlab.geocom.com.uy/api/v4/projects/2951/terraform/state/vault-qa/lock"
    username       = "btuduri"
    password       = "glpat-XP8Pa8FawxhCxskhLnEw"
    lock_method    = "POST"
    unlock_method  = "DELETE"
    retry_wait_min = 1
  }
}
