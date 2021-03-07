curl --request GET --header "content-type:application/json" https://fantasy-predictor.appspot.com/_ah/api/players/v1/5654600029831168
curl --request PUT --header "content-type:application/json" --data '{"aka": "delete"}' https://fantasy-predictor.appspot.com/_ah/api/players/v1/5654600029831168?updateMask=aka
curl --request PUT --header "content-type:application/json" --data '{"season": "2020-21"}' https://fantasy-predictor.appspot.com/_ah/api/players/v1/5654600029831168?updateMask=season

