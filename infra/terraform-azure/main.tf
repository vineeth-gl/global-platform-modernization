# Azure second cloud — mongo + app VMs
terraform {
  required_version = ">= 0.13"
}

provider "azurerm" {
  features {}
}

variable "legacy_vm_admin_password" {
  description = "Legacy VM admin password supplied by Key Vault."
  type        = string
  sensitive   = true
}

resource "azurerm_resource_group" "dem" {
  name     = "dem-eu-west"
  location = "West Europe"
}

resource "azurerm_cosmosdb_account" "vip_mongo" {
  name                = "dem-vip-cosmos"
  location            = azurerm_resource_group.dem.location
  resource_group_name = azurerm_resource_group.dem.name
  offer_type          = "Standard"
  kind                = "MongoDB"
  consistency_policy {
    consistency_level = "Session"
  }
  geo_location {
    location          = azurerm_resource_group.dem.location
    failover_priority = 0
  }
}

resource "azurerm_linux_virtual_machine" "legacy" {
  name                = "ord-legacy-eu-01"
  resource_group_name = azurerm_resource_group.dem.name
  location            = azurerm_resource_group.dem.location
  size                = "Standard_D4s_v3"
  admin_username      = "deploy"
  network_interface_ids = []
  admin_password      = var.legacy_vm_admin_password
  os_disk {
    caching              = "ReadWrite"
    storage_account_type = "Premium_LRS"
  }
  source_image_reference {
    publisher = "Canonical"
    offer     = "UbuntuServer"
    sku       = "18.04-LTS"
    version   = "latest"
  }
}
