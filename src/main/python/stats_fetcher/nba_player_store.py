import json
import Levenshtein
import re
import string
from bs4 import BeautifulSoup
from util import is_file_exist, is_int, send_request
from datetime import datetime
from multiprocessing import Pool
from dd4_service import DD4Service

file_path = "../../../../data/nba_players.jsonl"
overview_url = "https://www.basketball-reference.com/players/{}/{}.html"
dd4_service = DD4Service()

class PlayerStore:
  players = None

  def load(self):
    if not is_file_exist(file_path):
      self.fetch()
    else:
      self.players = []
      with open(file_path, 'r') as f:
        for line in f:
          self.players.append(json.loads(line))

  def get_all(self):
    if self.players is None:
      self.load()

    return self.players

  def get_active(self, year: int):
    active = []
    for player in self.get_all():
      if player['year_min'] <= year <= player['year_max']:
        active.append(player)
    return active

  def get(self, name: str):
    for player in self.get_all():
      if player['name'] == name:
        return player
    return None


  def fill_playoff_years(self, player: dict):
    playoff_years = []
    response = send_request(overview_url.format(player['id'][0], player['id']))
    soup = BeautifulSoup(response.content, 'html.parser')

    for row in soup.find_all("tr"):
      id = row.get('id', 'none')
      if id.startswith("per_game_stats_post.") and id[-4:] != '.Yrs':
        if row.find_all("th")[0].text != '':
          year = id[-4:]
          playoff_years.append(int(year))
    player['playoff_years'] = playoff_years

  def save(self):
    with open(file_path, "w", encoding="utf-8") as f:
      for player in self.players:
        json.dump(player, f, ensure_ascii=False, separators=(',', ':'))
        f.write("\n")

  def fetch(self):
    with open(file_path, "w", encoding="utf-8") as f:
      for letter in string.ascii_lowercase:
        if letter == 'x':
          continue
        print(letter)
        response = send_request(f"https://www.basketball-reference.com/players/{letter}/")
        soup = BeautifulSoup(response.content, 'html.parser')
        self.players = []

        rows = soup.find_all('tr')
        for row in rows:
          print(row)
          th = row.find_all('th')[0]
          id = th.get('data-append-csv')
          if id is not None:
            a = th.find_all('a')[0]
            name = a.string
            player = {"id": id, "name": name}
            tds = row.find_all('td')
            for td in tds:
              value = td.string
              if is_int(value):
                value = int(value)
              player[td.get('data-stat')] = value
            print(player)
            self.players.append(player)
            json.dump(player, f, ensure_ascii=False, separators=(',', ':'))
            f.write("\n")


def print_players():
  player_store = PlayerStore()
  active = player_store.get_all()
  count = 0
  for player in active:
    print(player)
    if player.get("playoff_years") is None:
      count += 1
      player_store.fill_playoff_years(player)
      if count % 100 == 0:
        player_store.save()
  # player_store.save()


def set_basket_ref_fields(player: dict, basket_ref_player: dict):
  player['basketRefId'] = basket_ref_player['id']
  player['dateOfBirth'] = int(datetime.strptime(basket_ref_player['birth_date'], "%B %d, %Y").timestamp()) * 1000
  player['minSeason'] = basket_ref_player['year_min']
  player['maxSeason'] = basket_ref_player['year_max']


def best_match(player: dict, options: list) -> dict:
  value = f"{player['name']}, {player['position']}, {player['height']}, {player['weight']}"
  closest = {'player': None, 'distance': 13}
  for p in options:
    distance = Levenshtein.distance(value, f"{p['name']}, {p['pos']}, {p['height']}, {p['weight']}")
    if closest is None or distance < closest['distance']:
      closest = {"player": p, "distance": distance}
  return closest['player']


def create(player):
  dd4_service.create('players', player)


def parse_nba_com(player_store: PlayerStore):
  basket_ref_players = player_store.get_all()
  by_name = {p["name"]: p for p in basket_ref_players}
  by_bask_ref_ids = {}
  players = []
  pattern = re.compile(r"/player/(\d+)/")
  with open("../../../../data/nba_com_players.html", "r", encoding="utf-8") as f:
    soup = BeautifulSoup(f.read(), 'html.parser')

    rows = soup.find_all('tr')
    for row in rows:
      # print(row)
      tds = row.find_all('td')
      if len(tds) == 0:
        continue
      link = tds[0].find_all('a')[0].get("href")
      player = {}
      match = pattern.search(link)
      if match:
        player['id'] = int(match.group(1))
      ps = tds[0].find_all('p')
      name = (ps[0].text + ' ' + ps[1].text).strip()
      player['name'] = name
      if len(tds[2].text.strip()) > 0:
        player['number'] = tds[2].text
      player['position'] = tds[3].text
      player['height'] = tds[4].text
      player['weight'] = tds[5].text
      player['school'] = tds[6].text
      player['country'] = tds[7].text

      basket_ref_player = by_name.get(name)
      if basket_ref_player is not None:
        set_basket_ref_fields(player, basket_ref_player)
        by_bask_ref_ids[basket_ref_player['id']] = True
      players.append(player)

  no_match = []
  for basket_ref_player in basket_ref_players:
    if by_bask_ref_ids.get(basket_ref_player['id']) is None:
      # print('No entry for: ', basket_ref_player['name'])
      no_match.append(basket_ref_player)

  for player in players:
    if player.get('basketRefId') is None:
      bm = best_match(player, no_match)
      if bm is not None:
        print(player['name'], 'not found', 'best match:', bm['name'])
        set_basket_ref_fields(player, bm)
      else:
        print(player['name'], 'not found and no best match')
    print(player['name'], player['school'])
  print('Players:', len(players))

  with Pool() as pool:
    results = pool.map(create, players)


if __name__ == '__main__':
  # print_players()
  ps = PlayerStore()
  parse_nba_com(ps)
  ps.save()


