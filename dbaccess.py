import tornado.database, random
from tornado.options import define, options
from game import Game
from pythonutils import SequenceOrderedDict

class DBAccess():
	define("mysql_host", default="localhost", help="localhost")
	define("mysql_database", default="wordgamedb", help="my database name")
	define("mysql_user", default="wordchain", help="database user")
	define("mysql_password", default="Chain!!32", help="database password")
	db = tornado.database.Connection(host=options.mysql_host, database=options.mysql_database, user=options.mysql_user, password=options.mysql_password)
	
	def getRegId(self, userid):
		cur = self.db.query("SELECT regid FROM gcm WHERE username=%s", userid)
		for row in cur:
			return row.regid
	def registerUser(self, regid, userid):
		if not self.db.query("SELECT EXISTS(SELECT 1 FROM gcm WHERE username=%s)", userid)[0].values()[0]:
			self.db.execute("INSERT INTO gcm (username, regid) VALUES (%s, %s)", userid, regid)
		else:
			self.db.execute("UPDATE gcm SET regid=%s WHERE username=%s", regid, userid)
	def unregisterUser(self, userid):
		self.db.execute("DELETE FROM gcm WHERE username = %s", userid)
	def getUserGames(self, userid):
		allGames = self.db.query("SELECT gameid FROM users WHERE userid=%s AND gameid IS NOT NULL AND finished != 1", userid)
		gameids = []
		for row in allGames:
			gameids.append(row.gameid)
		return gameids
	def getUserPic(self, userid):
		userCur = self.db.query("SELECT picurl FROM users WHERE userid=%s AND picurl is NOT NULL", userid)
		for row in userCur:
			return str(row.picurl)
	def getUserName(self, userid):
		userCur = self.db.query("SELECT displayname FROM users WHERE userid=%s", userid)
		for row in userCur:
			return str(row.displayname)
	def doesWordExist(self, word, gameid):
		if self.db.query("SELECT EXISTS(SELECT 1 FROM words WHERE username=%s AND gameid=%s)", username, gameid)[0].values()[0]:
			return True
		return False
	def insertWord(self, word, user, myorder, gameid):
		self.db.execute("INSERT INTO words (word, user, myorder, gameid) VALUES (%s, %s, %s, %s)", word, user, myorder, gameid)
	def insertScore(self, score, gameid, userid):
		self.db.execute("INSERT INTO scores (score, gameid, userid) VALUES (%s, %s, %s)", score, gameid, userid)	
	def insertGame(self, game):
		self.db.execute("INSERT INTO games (id, type, turncount, creator, mode) VALUES (%s, %s, %s, %s, %s)", game.id, game.mtype, game.turncount, game.creator, game.mode)
		for id in game.userids:
			self.insertUser(self.getUserName(id), id, game.id, game.userids.index(id), None)
		if (game.wordlist):
			for word, user in game.wordlist:
				self.db.execute("INSERT INTO words (word, gameid, user) VALUES (%s, %s, %s)", word, game.id, user)
	def isGameFinished(self, gameid):
		cursor = self.db.query("SELECT * FROM users WHERE gameid=%s", gameid)
		for row in cursor:
			if row.finished == 0:
				return False
		return True
	def insertUser(self, name, userid, gameid, turnorder, picurl):
		if (gameid != None):
			self.db.execute("INSERT INTO users (displayname, userid, gameid, turnorder, finished) VALUES (%s, %s, %s, %s, 0)", name, userid, gameid, turnorder)
		else:
			self.db.execute("INSERT INTO users (displayname, userid, picurl) VALUES (%s, %s, %s)", name, userid, picurl)
	def updateUser(self, name, userid, picurl):
		self.db.execute("UPDATE users SET displayname=%s, userid=%s, picurl=%s WHERE userid=%s", name, userid, picurl, userid)
	def updateFinished(self, userid, gameid):
		self.db.execute("UPDATE users SET finished=%s WHERE userid=%s AND gameid=%s", 1, userid, gameid)
	def deleteGame(self, gameid):
		self.db.execute("DELETE FROM games WHERE id=%s", gameid)
		self.db.execute("DELETE FROM words WHERE gameid=%s", gameid)
		self.db.execute("DELETE FROM users WHERE gameid=%s", gameid)
		self.db.execute("DELETE FROM scores WHERE gameid=%s", gameid)
	def userInDb(self, userid):
		if self.db.query("SELECT EXISTS(SELECT 1 FROM users WHERE userid=%s)", userid)[0].values()[0]:
			return True
		return False
	def updateTurnCount(self, game):
		self.db.execute("UPDATE games SET turncount=%s WHERE id=%s", game.turncount, game.id)
	def updateGame(self, game, newword, player, myorder):
		self.db.execute("UPDATE games SET turncount=%s WHERE id=%s", game.turncount, game.id)
		self.insertWord(newword, player, myorder, game.id)
	def isGameOver(self, gameid):
		cursor = self.db.query("SELECT * FROM scores WHERE gameid=%s", gameid)
		count = 0
		for row in cursor:
			count = count + 1
		if count > 1:
			return True
		return False
	def getScore(self, gameid, userid):
		cursor = self.db.query("SELECT * FROM scores WHERE gameid=%s AND userid=%s", gameid, userid)
		for row in cursor:
			return str(row.score)
	def getGame(self, gameid):
		gameCur = self.db.query("SELECT * FROM games WHERE id=%s", gameid)
		mtype = ''
		mode = ''
		id = ''
		creator = ''
		turncount = 0
		for row in gameCur:
			mtype = row.type
			mode = row.mode
			id = row.id
			creator = row.creator
			turncount = row.turncount
		userList = []
		userids = []
		wordsList = SequenceOrderedDict()
		userCur = self.db.query("SELECT userid, displayname FROM users WHERE gameid=%s ORDER BY turnorder", gameid)
		for user in userCur:
			userList.append(user.displayname)
			userids.append(user.userid)
		wordsCur = self.db.query("SELECT word, user FROM words WHERE gameid=%s ORDER BY myorder", gameid)
		for wordRow in wordsCur:
			wordsList[wordRow.word] = wordRow.user
		return Game(userids, userList, mtype, id, creator, turncount, wordsList, mode)
