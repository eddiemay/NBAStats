com.digitald4.nbastats.LineUpsCtrl = function(globalData, lineUpService, playerDayService) {
  globalData.controlType = globalData.CONTROL_TYPE.DATE
	globalData.refresh = this.refresh.bind(this);
	this.globalData = globalData;
	this.lineUpService = lineUpService;
	this.playerDayService = playerDayService;
	this.refresh();
	this.sortProp = 'projected';
  this.reverse = true;
};

com.digitald4.nbastats.LineUpsCtrl.prototype.assemble = function() {
  for (var i = 0; i < this.lineUps.length; i++) {
    var lineUp = this.lineUps[i];
    lineUp.player = [];
    var league = lineUp.fantasySite;
    var method = lineUp.projectionMethod;
    for (var p = 0; p < lineUp.playerId.length; p++) {
      var player = this.playerDayMap[lineUp.playerId[p]];
      lineUp.player.push({
        playerId: player.playerId,
        name: player.name,
        cost: player.fantasySiteInfo[league].cost,
        projected: player.fantasySiteInfo[league].projection[method],
        actual: player.fantasySiteInfo[league].actual,
      });
    }
  }
};

com.digitald4.nbastats.LineUpsCtrl.prototype.refresh = function() {
  this.lineUps = null;
  this.playerDayMap = null;
  var date = this.globalData.getApiDate();
  var league = this.globalData.fantasyLeague;
  this.playerDayService.list(date, function(response) {
    var playerDayMap = {};
    for (var i = 0; i < response.results.length; i++) {
      var playerDay = response.results[i];
      playerDayMap[playerDay.playerId] = playerDay;
      if (i == 0) {
        this.projectionMethods = [];
        for (var projectionMethod in playerDay.fantasySiteInfo[league].projection) {
          this.projectionMethods.push({name: projectionMethod});
        }
        this.projectionMethods.push({name: 'Actual'});
      }
    }
    this.playerDayMap = playerDayMap;
    if (this.lineUps) {
      this.assemble();
    }
  }.bind(this), notify);

  this.refreshLineUps();
};

com.digitald4.nbastats.LineUpsCtrl.prototype.refreshLineUps = function() {
  var date = this.globalData.getApiDate();
  var league = this.globalData.fantasyLeague;
	this.lineUpService.list(date, league, this.globalData.projectionMethod, function(response) {
	  this.lineUps = response.results;
    if (this.playerDayMap) {
      this.assemble();
    }
	}.bind(this), notify);
};

com.digitald4.nbastats.LineUpsCtrl.prototype.update = function(lineUp, prop) {
  var index = this.lineUps.indexOf(lineUp);
	this.lineUpService.update(lineUp, [prop], function(lineUp) {
	  this.lineUps[index] = lineUp;
	  this.assemble();
	}.bind(this), notify);
};

com.digitald4.nbastats.LineUpsCtrl.prototype.updateActuals = function() {
  this.lineUpService.updateActuals(this.globalData.getApiDate(), function(response) {
    console.log('Processing...');
  }.bind(this), notify);
};

com.digitald4.nbastats.LineUpsCtrl.prototype.sortBy = function(prop) {
  if (this.sortProp == prop) {
    this.reverse = !this.reverse;
  } else {
	  this.sortProp = prop;
	  this.reverse = false;
	}
};
