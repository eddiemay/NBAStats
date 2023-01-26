com.digitald4.nbastats.PlayerOptionsCtrl = function(globalData, playerDayService) {
	globalData.controlType = globalData.CONTROL_TYPE.DATE
	globalData.refresh = this.refresh.bind(this);
	this.globalData = globalData;
	this.playerDayService = playerDayService;
	this.refresh();
	this.sortProp = 'projection[\'RotoG Proj\']';
  this.reverse = true;
};

com.digitald4.nbastats.PlayerOptionsCtrl.prototype.refresh = function() {
  this.players = [];
  this.projectionMethods = [];
  var fantasyLeague = this.globalData.fantasyLeague;
	this.playerDayService.listByDate(this.globalData.getApiDate(), response => {
	  if (response.items.length == 0) {
	    return;
	  }
	  for (var method in response.items[0].fantasySiteInfo[fantasyLeague].projection) {
	    this.projectionMethods.push(method);
	  }
	  for (var i = 0; i < response.items.length; i++) {
	    var player = response.items[i];
	    this.players.push({
	      playerId: player.playerId,
	      name: player.name,
	      status: player.status,
	      team: player.team,
	      opponent: player.opponent,
	      position: player.fantasySiteInfo[fantasyLeague].position.join('/'),
	      cost: player.fantasySiteInfo[fantasyLeague].cost,
	      actual: player.fantasySiteInfo[fantasyLeague].actual,
	      projection: player.fantasySiteInfo[fantasyLeague].projection,
	    });
	  }
	}, notifyError);
}

com.digitald4.nbastats.PlayerOptionsCtrl.prototype.update = function(player, prop) {
  var index = this.players.indexOf(player);
  this.playerDayService.update(player, [prop], player => {
    this.players[index] = player;
  }, notifyError);
}

com.digitald4.nbastats.PlayerOptionsCtrl.prototype.processStats = function() {
  this.playerDayService.processStats(this.globalData.getApiDate(), response => {
    console.log('Processing...');
  }, notifyError);
};

com.digitald4.nbastats.PlayerOptionsCtrl.prototype.sortBy = function(prop) {
  if (this.sortProp == prop) {
    this.reverse = !this.reverse;
  } else {
	  this.sortProp = prop;
	  this.reverse = false;
	}
};
