com.digitald4.nbastats.LineUpsCtrl = function(globalData, lineUpService, playerDayService) {
  globalData.controlType = globalData.CONTROL_TYPE.DATE
	globalData.refresh = this.refresh.bind(this);
	this.globalData = globalData;
	this.lineUpService = lineUpService;
	this.playerDayService = playerDayService;
	this.refresh();
	this.sortProp = 'projected';
  this.reverse = true;
}

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
}

com.digitald4.nbastats.LineUpsCtrl.prototype.refresh = function() {
  this.lineUps = null;
  this.playerDayMap = null;
  var date = this.globalData.getApiDate();
  var league = this.globalData.fantasyLeague;
  this.playerDayService.listByDate(date, response => {
    var playerDayMap = {};
    for (var i = 0; i < response.items.length; i++) {
      var playerDay = response.items[i];
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
  }, notifyError);

  this.refreshLineUps();
}

com.digitald4.nbastats.LineUpsCtrl.prototype.refreshLineUps = function() {
  var date = this.globalData.getApiDate();
  var league = this.globalData.fantasyLeague;
	this.lineUpService.listByDateSiteMethod(
	    date, league, this.globalData.projectionMethod, response => {
	  this.lineUps = response.items;
    if (this.playerDayMap) {
      this.assemble();
    }
	}, notifyError);
}

com.digitald4.nbastats.LineUpsCtrl.prototype.update = function(lineUp, prop) {
  var index = this.lineUps.indexOf(lineUp);
	this.lineUpService.update(lineUp, [prop], lineUp => {
	  this.lineUps[index] = lineUp;
	  this.assemble();
	}, notifyError);
}

com.digitald4.nbastats.LineUpsCtrl.prototype.updateActuals = function() {
  this.lineUpService.updateActuals(this.globalData.getApiDate(), response => {
    console.log('Processing...');
  }, notifyError);
}

com.digitald4.nbastats.LineUpsCtrl.prototype.sortBy = function(prop) {
  if (this.sortProp == prop) {
    this.reverse = !this.reverse;
  } else {
	  this.sortProp = prop;
	  this.reverse = false;
	}
}
