com.digitald4.nbastats.PlayersCtrl = function(globalData, playerService) {
  this.globalData = globalData;
	globalData.controlType = globalData.CONTROL_TYPE.SEASON;
	globalData.refresh = this.refresh.bind(this);
	this.playerService = playerService;
	this.refresh();
}

com.digitald4.nbastats.PlayersCtrl.prototype.refresh = function() {
  this.playerService.listBySeason(this.globalData.getSeason(), response => {
    this.players = response.items;
  }, notifyError);
}

com.digitald4.nbastats.PlayersCtrl.prototype.update = function(player, prop) {
  var index = this.players.indexOf(player);
  this.playerService.update(player, [prop], player => {
    this.players[index] = player;
  }, notifyError);
}