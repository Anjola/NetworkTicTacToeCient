ASSIGNMENT 4: TIC TAC TOE CLIENT 
This is the android client for the network tic tac toe game. The 
app uses a bound service for all the networking activities and uses 
the messenger to deliver things back to the client. The communication
logic is essentially the same as the group chat server. For actual 
communication, the app uses the MSG command from the group chat server

*************************************************************************
PROTOCOL DESIGN 
*************************************************************************
The user is registered once the application is launched and named with 
the clientID to conform to the server. A new game can be requested as X,
O or random. On this request, the client is put in a corresponding wait
group by the server by sending the JOIN command. The server responds with
a game_id group that contains only the two players once an opponent is 
found.  The MSG command of the group chat server is then used for turn 
by turn communication by sending MOVE,x coord,y coord. Once the other
client receives a MOVE command, it checks if the move makes the game 
over, if not, it swaps turns to itself so that any message received 
during that period is ignored until it plays. Buttons are disabled once
they are used and on swapped turns to minimize repeat requests. The 
client can always poll after going offline and changing endpoints.
Win/tie logic is completely determined on the client. 


*************************************************************************
REQUEST COMMANDS
*************************************************************************
REGISTER,<Optional User ID>
	Creates a new user on the received endpoint. Including the client ID 
	indicates that this is a returning user from a different endpoint. 
	Changes the endpoint of the user with argument passed in 
	
NAME,<User ID>,<User ID>
	Gives the corresponding user its ID as its name
	
JOIN,<User ID>,<waiting group nmae>
	adds the corresponding user to the requested waiting group or creates  
	group if it does not already exist. 


MSG,<User ID>,<game_id>,MOVE,<x cord>,<y cord>
	tells the other opponent in group game_id to place clients move in 
	position x cord,y cord. Should not allow invalid moves.  
	
POLL,<User ID>
	Requests undelivered messages  
 

ACK,<Acknowlege ID>
	Clients way of telling server it received the message that has the 
	specified acknowlege ID(ClientID:RequestNumber)

	
*************************************************************************
RECEIPT COMMANDS
*************************************************************************

 MOVE,<x cord>,<y cord>
	plays opponent piece in passed in coordinates. 

 ACK,<Acknowlege ID>
	Server;s way of telling client it received the message that has the 
	specified acknowlege ID(RequestNumber)
	
REGISTERED
	Lets client know it is registered on the server. Client requests name
	and waits for new game to be initiated. 
	
game_<game_id>
	signifies opponent found and communication can proceed between 
	oppoonents through the group game_id
	