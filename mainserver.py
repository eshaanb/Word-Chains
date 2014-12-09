import tornado.ioloop, tornado.web, tornado.httpserver, tornado.options, json, urllib, tornado.httputil, tornado.escape, time, threading, re, random
import gcm.gcm, logging
import uuid
from game import Game
from pythonutils import SequenceOrderedDict
from dbaccess import DBAccess

mydba = DBAccess()
logging.basicConfig(filename='access.log', level=logging.INFO, filemode='a+',
format='%(asctime)s,%(msecs)d %(name)s %(levelname)s %(message)s',
datefmt='%H:%M:%S')

myGcm = gcm.gcm.GCM('AIzaSyDq_TU8iq2vQ4nLOzEfCDf8Z0Uk51xXQCQ')

class Application(tornado.web.Application):
	def __init__(self):
		handlers = [
			(r'/register', RegisterHandler),
			(r'/checkregister', WhichAreRegisteredUsers),
			(r'/feedback', FeedbackHandler),
            (r'/checkuser', CheckUserHandler),
			(r'/getgamelist', GetGameListHandler),
			(r'/newgame', NewGameHandler),
			(r'/forfeit', ForfeitGameHandler),
			(r'/domove', DoMoveHandler),
			(r'/getlastword', GetLastWordHandler),
			(r'/getwhoseturn', GetWhoseTurnHandler),
			(r'/gcm', GCMRegisterHandler),
			(r'/gcmunregister', GCMUnregisterHandler),
			(r'/finished', FinishedHandler),
			(r'/timeattackmove', DoTimeAttackMoveHandler),
		]
		tornado.web.Application.__init__(self, handlers)
		
class RegisterHandler(tornado.web.RequestHandler):
	def post(self):
		if self.get_argument('userid', '') and self.get_argument('name', '') and self.get_argument('picurl', ''):
			self.userid = str(self.get_argument('userid'))
			self.displayname = str(self.get_argument('name'))
			self.picurl = str(self.get_argument('picurl'))
			if not mydba.userInDb(self.userid):
				mydba.insertUser(self.displayname, self.userid, None, None, self.picurl)
			else:
				mydba.updateUser(self.displayname, self.userid, self.picurl)
		else:
			self.write('Please enter a non-empty email')
			self.finish()
            
class CheckUserHandler(tornado.web.RequestHandler):
    def get(self):
        if self.get_argument('userid', ''):
            self.username = str(self.get_argument('userid'))
            if mydba.userInDb(self.username):
                self.write('yes')
                self.finish()
            else:
                self.write('no')
                self.finish()
        else:
            self.write('Please enter a non-empty userid')
            self.finish()
		
class GetGameListHandler(tornado.web.RequestHandler):
	def get(self):
		if self.get_argument('userid', '') and self.get_argument('currver', ''):
			if float(self.get_argument('currver', '')) < 1.0:
				self.write("client out of date")
				self.finish();
			else:
				self.userid = str(self.get_argument('userid'))
				bigDict = {'games' : []}
				for gameid in mydba.getUserGames(self.userid): #loop through all the user's game ids
					tempGame = mydba.getGame(gameid)
					tempDict = tempGame.toDict(True)
					if tempGame.mode.lower() == 'time attack':
						oppid = None
						for userid in tempGame.userids:
							if self.userid != userid: #if sent id does not equal given id in loop - then it is opp id
								oppid = userid
								break
						tempDict['myscore'] = mydba.getScore(tempGame.id, self.userid)
						tempDict['oppscore'] = mydba.getScore(tempGame.id, oppid)
					for id in tempGame.userids:
						if id != self.userid:
							tempDict['picurl'] = mydba.getUserPic(id)
							break
					bigDict['games'].append(tempDict) #get game from game id
				self.write(bigDict)
				self.finish();
		else:
			self.write('Please supply a valid email')
			self.finish()

class NewGameHandler(tornado.web.RequestHandler):
	def post(self):
		if self.get_argument('userid', '') and self.get_argument('oppname', '') and self.get_argument('mode', '') and self.get_argument('oppid', '') and self.get_argument('gametype', ''):
			self.gametype = str(self.get_argument('gametype'))
			self.userid = str(self.get_argument('userid'))
			self.oppid = str(self.get_argument('oppid'))
			self.oppname = str(self.get_argument('oppname'))
			self.mode = str(self.get_argument('mode'))
			tempList = [self.userid, self.oppid]
			userNames = []
			for id in tempList:
				userNames.append(mydba.getUserName(id))
			newGameId = uuid.uuid4().hex
			game = Game(tempList, userNames, self.gametype, newGameId, self.userid, 0, SequenceOrderedDict(), self.mode)
			mydba.insertGame(game)
			data = {}
			mregid = mydba.getRegId(self.oppid)
			if mregid:
				data = game.toDict(False)
				data['picurl'] = mydba.getUserPic(self.userid)
				try:
					myGcm.plaintext_request(registration_id=mregid, data={"game" : data})
				except:
					pass
			self.write(newGameId)
			self.finish()
		else:
			self.write('Invalid Parameters!')
			self.finish()

class ForfeitGameHandler(tornado.web.RequestHandler): #TODO: Implement forfeit handler in client and in server using databases
	def put(self):
		if self.get_argument('userid', '') and self.get_argument('gameid', ''):
			self.gameid = str(self.get_argument('gameid'))
			self.userid = str(self.get_argument('userid'))
			g = mydba.getGame(self.gameid)
			for userid in g.userids:
				if userid != self.userid:
					mregid = mydba.getRegId(userid)
					if mregid:
						try:
							data = {"type" : g.mtype, "gameid" : self.gameid, "playername" : mydba.getUserName(self.userid), "forfeit" : "yes"}
							myGcm.plaintext_request(registration_id=mregid, data=data)
						except:
							pass
			mydba.deleteGame(self.gameid)
			self.write('forfeit successful')
			self.finish()
		else:
			self.write('Please supply a valid email and gameid')
			self.finish()

class DoMoveHandler(tornado.web.RequestHandler):
	def put(self):
		if self.get_argument('userid', '') and self.get_argument('word', '') and self.get_argument('gameid', ''):
			self.word = str(self.get_argument('word'))
			self.userid = str(self.get_argument('userid'))
			self.id = str(self.get_argument('gameid'))
			game = mydba.getGame(self.id)
			if len(game.userids) > 0:
				for word in game.wordlist:
					if word.lower() == self.word.lower():
						self.write("This word has already been played!")
						self.finish()
						return
				game.wordlist[self.word] = self.userid
				game.incrementTurn()
				mydba.updateGame(game, self.word, self.userid, game.wordlist.index(self.word))
				mregid = mydba.getRegId(game.userids[game.turncount])
				if mregid:
					try:
						data = {"wordindex" : game.wordlist.index(self.word), "currplayer" : game.userids[game.turncount], "playername" : mydba.getUserName(self.userid), "playerid" : self.userid, "gameid" : game.id, "word" : game.getLastWord()}
						myGcm.plaintext_request(registration_id=mregid, data=data)
					except:
						pass
			else:
				self.write("Opponent has forfeited.")
				self.finish()
		else:
			self.write('Please supply a valid email')
			self.finish()
			
class DoTimeAttackMoveHandler(tornado.web.RequestHandler):
	def put(self):
		if self.get_argument('userid', '') and self.get_argument('score', '') and self.get_argument('gameid', ''):
			self.score = str(self.get_argument('score'))
			self.userid = str(self.get_argument('userid'))
			self.id = str(self.get_argument('gameid'))
			game = mydba.getGame(self.id)
			if len(game.userids) > 0:
				mydba.insertScore(self.score, self.id, self.userid);
				if (mydba.isGameOver(self.id)):
					game.incrementTurn()
					mregid = mydba.getRegId(game.userids[game.turncount])
					if mregid:
						try:
							data = {"gameover" : True, "playername" : mydba.getUserName(self.userid), "playerid" : self.userid, "gameid" : game.id, "score" : self.score}
							myGcm.plaintext_request(registration_id=mregid, data=data)
						except:
							pass
					game.turncount = -1
				else:
					game.incrementTurn()
					mregid = mydba.getRegId(game.userids[game.turncount])
					if mregid:
						try:
							data = {"gameover" : False, "playername" : mydba.getUserName(self.userid), "playerid" : self.userid, "gameid" : game.id, "score" : self.score}
							myGcm.plaintext_request(registration_id=mregid, data=data)
						except:
							pass
				mydba.updateTurnCount(game)
			else:
				self.write("Opponent has forfeited.")
				self.finish()
		else:
			self.write('Please supply a valid email')
			self.finish()

class FinishedHandler(tornado.web.RequestHandler):
	def post(self):
		if self.get_argument('userid', '') and self.get_argument('gameid', ''):
			self.gameid = str(self.get_argument('gameid'))
			self.userid = str(self.get_argument('userid'))
			mydba.updateFinished(self.userid, self.gameid);
			if (mydba.isGameFinished(self.gameid)):
				mydba.deleteGame(self.gameid)
		else:
			self.write('Please supply a valid email and gamename')
			self.finish()
			
class GetLastWordHandler(tornado.web.RequestHandler):
	def get(self):
		if self.get_argument('gameid', ''):
			self.gameid = str(self.get_argument('gameid'))
			game = mydba.getGame(self.gameid)
			self.write(game.wordlist[len(game.wordlist)-1:len(game.wordlist)].keys()[0])
			self.finish()
		else:
			self.write('Please supply a valid email and gamename')
			self.finish()

class GetWhoseTurnHandler(tornado.web.RequestHandler):
	def get(self):
		if self.get_argument('gameid', ''):
			self.gameid = str(self.get_argument('gameid'))
			game = mydba.getGame(self.gameid)
			self.write(game.userids[game.turncount])
			self.finish()
		else:
			self.write('Please supply a valid email and gamename')
			self.finish()

class GCMRegisterHandler(tornado.web.RequestHandler):
	def post(self):
		if self.get_argument('regId', '') and self.get_argument('userid', ''):
			regId = self.get_argument('regId');
			email = self.get_argument('userid');
			mydba.registerUser(regId, email);
		else:
			self.write('Please supply a valid regId and email')
			self.finish()

class GCMUnregisterHandler(tornado.web.RequestHandler):
	def post(self):
		if self.get_argument('userid', ''):
			email = self.get_argument('userid');
			mydba.unregisterUser(email);
		else:
			self.write('Please supply a valid email')
			self.finish()				
			
class FeedbackHandler(tornado.web.RequestHandler):
	def post(self):
		if self.get_argument('feedback', ''):
			fdbck = self.get_argument('feedback');
			feedback = open('feedback.txt', 'a+')
			feedback.write(fdbck+"\n")
			feedback.close()
		elif self.get_argument('name', '') and self.get_argument('message', ''):
			name = self.get_argument('name');
			message = self.get_argument('message');
			feedback = open('feedback.txt', 'a+')
			feedback.write(name+': '+message+'\n')
			feedback.close()
			self.write('Message Received!')
			self.finish()
		else:
			self.write('Invalid Parameters')
			self.finish()
			
class WhichAreRegisteredUsers(tornado.web.RequestHandler):
	def get(self):
		if self.get_argument('csvusers', ''):
			users = str(self.get_argument('csvusers'));
			returnString = ''
			for userid in users.split(","):
				if mydba.userInDb(userid):
					returnString += userid+","
			self.write(returnString)
			self.finish();
		else:
			self.write('Invalid Parameters')
			self.finish()
			

def main():
	tornado.options.parse_command_line()
	http_server = tornado.httpserver.HTTPServer(Application())
	http_server.listen(8888)
	tornado.ioloop.IOLoop.instance().start()
		
if __name__ == '__main__':
    main()
