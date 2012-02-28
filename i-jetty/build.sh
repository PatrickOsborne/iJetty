set -x

mvn clean install
retVal=$?
if [ $retVal -ne 0 ]; then
        exit $retVal
fi

pushd i-jetty-ui
mvn android:deploy
retVal=$?
popd
exit $retVal