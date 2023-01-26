com.digitald4.nbastats.module = angular.module('nbastats', ['DD4Common', 'ngRoute', 'ui.date'])
    .config(com.digitald4.nbastats.router)
    .controller('NBAStatsCtrl', function(globalData, $filter) {
      globalData.CONTROL_TYPE = {
        NONE: 0,
        DATE: 1,
        SEASON: 2
      };
      globalData.controlType = globalData.CONTROL_TYPE.NONE;
      globalData.FANTASY_LEAGUE = {
        FAN_DUEL: 'fanduel',
        DRAFT_KINGS: 'draftkings',
      };
      globalData.fantasyLeague = globalData.FANTASY_LEAGUE.FAN_DUEL;
      globalData.date = new Date();
      globalData.baseYear = globalData.date.getFullYear();
      if (globalData.date.getMonth() < 7) {
        globalData.baseYear--;
      }
      globalData.prev = function() {
        if (globalData.controlType == globalData.CONTROL_TYPE.SEASON) {
          globalData.baseYear--;
        } else if (globalData.controlType == globalData.CONTROL_TYPE.DATE) {
          globalData.date.setDate(globalData.date.getDate() - 1);
        }
        globalData.refresh();
      };
      globalData.next = function() {
        if (globalData.controlType == globalData.CONTROL_TYPE.SEASON) {
          globalData.baseYear++;
        } else if (globalData.controlType == globalData.CONTROL_TYPE.DATE) {
          globalData.date.setDate(globalData.date.getDate() + 1);
        }
        globalData.refresh();
      };
      globalData.getSeason = function() {
        return globalData.baseYear + '-' + ((globalData.baseYear + 1) % 100);
      };
      globalData.getApiDate = function() {
        return $filter('date')(globalData.date, 'yyyy-MM-dd');
      };
    })
    .service('playerService', function(apiConnector) {
      var service = new com.digitald4.common.JSONService('player', apiConnector);
      service.listBySeason = function(season, onSuccess, onError) {
        service.list({filter: "season=" + season}, onSuccess, onError);
      };
      return service;
    })
    .service('playerDayService', function(apiConnector) {
      var service = new com.digitald4.common.JSONService('playerDay', apiConnector);
      service.listByDate = function(date, onSuccess, onError) {
        service.list({filter: "date=" + date}, onSuccess, onError);
      };
      service.processStats = function(date, onSuccess, onError) {
        this.sendRequest(
            {action: 'processStats', method: 'POST', data: {date: date}},
            onSuccess, onError);
      };
      return service;
    })
    .service('lineUpService', function(apiConnector) {
      var service = new com.digitald4.common.JSONService('lineUp', apiConnector);
      service.listByDateSiteMethod = function(date, site, method, onSuccess, onError) {
        service.list(
            {filter: "date=" + date + "&fantasySite=" + site + "&projectionMethod=" + method},
            onSuccess, onError);
      };
      service.updateActuals = function(date, onSuccess, onError) {
        this.sendRequest(
            {action: 'updateActuals', method: 'POST', data: {date: date}},
            onSuccess, onError);
      };
      return service;
    });
