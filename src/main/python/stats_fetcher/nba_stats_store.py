import json
from bs4 import BeautifulSoup
from nba_player_store import PlayerStore
from util import is_int, is_float, is_file_exist, send_request
from fake_useragent import UserAgent

ua = UserAgent()

file_path = "../../../../data/nba_player_game_stats_{}.jsonl"
playoffs_file_path = "../../../../data/nba_player_playoff_stats_{}.jsonl"
game_log_url = "https://www.basketball-reference.com/players/{}/{}/gamelog/{}/"

class StatsStore:
  def __init__(self, player_store: PlayerStore):
    self.player_store = player_store

  def get_stats(self, year: int, playoffs: bool, preprocessor: callable(dict) = lambda a:a):
    file = file_path.format(year) if not playoffs else playoffs_file_path.format(year)
    if not is_file_exist(file):
      self.fetch(year)

    stats = []
    with open(file, 'r') as f:
      for line in f:
        stats.append(preprocessor(json.loads(line)))

    return stats


  def resave(self, year: int, playoffs: bool):
    stats = self.get_stats(year, playoffs)
    file = file_path.format(year) if not playoffs else playoffs_file_path.format(year)
    with open(file, 'w') as f:
      for stat in stats:
        json.dump(stat, f, ensure_ascii=False, separators=(',', ':'))
        f.write("\n")

  def fetch(self, year: int):
    file = file_path.format(year)
    playoffs_file = playoffs_file_path.format(year)
    # Create a map of the players we have stats for.
    reg_player_map = {}
    if is_file_exist(file):
      with open(file, 'r') as f:
        for line in f:
          stats = json.loads(line)
          reg_player_map[stats['id']] = True

    # Create a map of the players we have playoff stats for.
    playoffs_player_map = {}
    if is_file_exist(playoffs_file):
      with open(playoffs_file, 'r') as f:
        for line in f:
          stats = json.loads(line)
          playoffs_player_map[stats['id']] = True

    with open(file, "a", encoding="utf-8") as f:
      with open(playoffs_file, "a", encoding="utf-8") as pf:
        players = self.player_store.get_active(year)
        print(len(players), "players for", year)
        for player in players:
          if reg_player_map.get(player['id']) is None or playoffs_player_map.get(player['id']) is None and year in player['playoff_years']:
            # Any player we don't have stats for should be appended to the file.
            player_stats = self.fetch_for_player(player, year)
            if reg_player_map.get(player['id']) is None:
              for player_stat in player_stats['regular']:
                json.dump(player_stat, f, ensure_ascii=False, separators=(',', ':'))
                f.write("\n")
            if playoffs_player_map.get(player['id']) is None:
              for player_stat in player_stats['playoffs']:
                json.dump(player_stat, pf, ensure_ascii=False, separators=(',', ':'))
                pf.write("\n")

  def fetch_playoffs(self, player: dict):
    playoff_years = player.get("playoff_years")
    if playoff_years is None:
      self.player_store.fill_playoff_years(player)
      self.player_store.save()

    for year in playoff_years:
      playoffs_file = playoffs_file_path.format(year)
      need_fetch = True
      if is_file_exist(playoffs_file):
        with open(playoffs_file, 'r') as f:
          for line in f:
            stats = json.loads(line)
            if stats['id'] == player['id']:
              need_fetch = False

      if need_fetch:
        player_stats = self.fetch_for_player(player, year)
        with open(playoffs_file, "a", encoding="utf-8") as pf:
          for player_stat in player_stats['playoffs']:
            json.dump(player_stat, pf, ensure_ascii=False, separators=(',', ':'))
            pf.write("\n")
          pf.close()

  def fetch_for_player(self, player: dict, year: int):
    print(player)
    response = send_request(game_log_url.format(player['id'][0], player['id'], year))
    soup = BeautifulSoup(response.content, 'html.parser')
    return {"regular": self.parse_for_player(player, soup, False),
            "playoffs": self.parse_for_player(player, soup, True)}

  def parse_for_player(self, player: dict, soup: BeautifulSoup, playoffs: bool):
    tag_id = "player_game_log_reg" if not playoffs else "player_game_log_post"

    player_game_log_reg = soup.find(id=tag_id)
    if player_game_log_reg is None:
      return []

    stats = []
    for row in player_game_log_reg.find_all("tr"):
      if not row.get('id', 'none').startswith(tag_id + "."):
        continue
      game_stat = {"id": player['id'], "name": player['name']}
      tds = row.find_all('td')
      for td in tds:
        value = td.string
        if is_int(value):
          value = int(value)
        elif is_float(value):
          value = float(value)
        game_stat[td.get('data-stat')] = value
      print(game_stat)
      stats.append(game_stat)

    return stats


if __name__ == '__main__':
  player_store = PlayerStore()
  stats_store = StatsStore(player_store)
  for year in range(1947, 2026):
    print(f"{year}")
    # stats_store.fetch(year)
    # stats_store.resave(year, False)
    # stats_store.resave(year, True)
    print(f"{year}:", len(stats_store.get_stats(year, False)), "reg season rows")
    print(f"{year}:", len(stats_store.get_stats(year, True)), "playoff rows")

  # stats_store.resave(2017, False)
