WORDLIST_KEY = 'wordlist'
TYPE_KEY = 'type'
ID_KEY = 'uniqueid'
USERLIST_KEY = 'userlist'
MODE_KEY = 'mode'
USERIDS_KEY = 'userids'
CURRPLAYER_KEY = 'currplayer'
GAMECREATOR_KEY = 'gamecreator'


class Game():
	def __init__(self, userids, userList, gametype, uniqueRandom, creator, turncount, wordlist, mode):
		self.mtype = gametype
		self.userids = userids
		self.mode = mode
		self.creator = creator
		self.id = uniqueRandom
		self.wordlist = wordlist
		self.turncount = turncount
		self.userList = userList
	
	def incrementTurn(self):
		if (self.turncount+1 < len(self.userList)):
			self.turncount += 1
			return
		else:
			self.turncount = 0
			
	def getLastWord(self):
		return self.wordlist[len(self.wordlist)-1:len(self.wordlist)].keys()[0]
			
	def toDict(self, sendWordList):
		returnDict = {}
		if self.turncount == -1:
			returnDict[CURRPLAYER_KEY] = None
		else:
			returnDict[CURRPLAYER_KEY] = self.userids[self.turncount]
		returnDict[MODE_KEY] = self.mode
		returnDict[USERIDS_KEY] = self.userids
		returnDict[GAMECREATOR_KEY] = self.creator
		returnDict[TYPE_KEY] = self.mtype
		returnDict[ID_KEY] = self.id
		returnDict[USERLIST_KEY] = self.userList
		if sendWordList:
			newWordList = {}
			for word, user in self.wordlist.items():
				newWordList[self.wordlist.index(word)] = {word : user}
			returnDict[WORDLIST_KEY] = newWordList
		return returnDict
		
		