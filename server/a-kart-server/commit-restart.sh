if [ $# -ne 1 ]; then
  echo "Usage ./commit-restart.sh <commit message>"
  exit 1
else
  git add -A .
  git commit -m "$1"
  git push
  ssh -i ~/edisaverio-aws-keys.pem ec2-user@ec2-52-28-225-211.eu-central-1.compute.amazonaws.com './startServer.sh'
fi

exit 0
