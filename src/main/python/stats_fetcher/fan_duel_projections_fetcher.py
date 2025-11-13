import requests
from bs4 import BeautifulSoup


def get_player_options():
  response = requests.get('https://www.fanduel.com/research/nba/fantasy/dfs-projections')
  print(response.content)
  soup = BeautifulSoup(response.content, 'html.parser')

  for row in soup.find_all("tr"):
    print(row)

if __name__ == '__main__':
  get_player_options()