#!/bin/bash
# VM-based deployment for legacy hosts that never moved to k8s
# Hosts: ord-legacy-03 (US), ord-legacy-eu-01 (Azure VM)
set -euo pipefail
HOST=${1:-ord-legacy-03}
COLOR=${2:-blue}
echo "Deploying monolith to VM $HOST track=$COLOR"
scp -r apps/monolith-core "deploy@$HOST:/opt/dem/monolith-$COLOR/"
ssh "deploy@$HOST" "sudo systemctl restart dem-monolith@$COLOR"
# flip VIP after health — blue-green on VMs
ssh "deploy@$HOST" "sudo /opt/dem/scripts/flip_vip.sh $COLOR"
echo "Done. Remember HA pair in ap-south still on weekly crontab."
