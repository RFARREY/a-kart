if [ $# -ne 3 ]; then
  echo "Usage ./commit-restart.sh <aws.creds.pem> <aws.host> <commit message>"
  exit 1
else
  git add -A .
  git commit -m "$3"
  git push
  echo "ssh -i $1 $2 './startServer.sh'"
fi

exit 0
