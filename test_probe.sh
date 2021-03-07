curl --request GET --header "content-type:application/json" https://fantasy-predictor.appspot.com/_ah/api/players/v1/5654600029831168
curl --request GET --header "content-type:application/json" https://fantasy-predictor.appspot.com/_ah/api/players/v1/_?pageSize=5&filter=season%3D2020-21
curl --request PUT --header "content-type:application/json" --data '{"aka": "deleted"}' https://fantasy-predictor.appspot.com/_ah/api/players/v1/5654600029831168?updateMask=aka
curl --request GET --header "content-type:application/json" https://fantasy-predictor.appspot.com/_ah/api/players/v1/5654600029831168
curl --request PUT --header "content-type:application/json" --data '{"aka": "delete"}' https://fantasy-predictor.appspot.com/_ah/api/players/v1/5654600029831168?updateMask=aka

