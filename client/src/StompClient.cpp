

#include "../include/StompClient.h"
#include <iostream>
#include <sstream>
void split_str(const std::string &s, char delim, std::vector<std::string> &elems) {
    std::stringstream ss(s);
    std::string item;
    while (std::getline(ss, item, delim)) {
        elems.push_back(item);
    }
}

StompClient::StompClient() : connectionHandler(nullptr), stompProtocol(), listenerThread(), mtx(), isRunning(false), isConnected(false) {
}

StompClient::~StompClient()
{
	stop(); // Ensure resources are released on destruction
}


void StompClient::run()
{
	isRunning = true;

	string input;

	while (isRunning && getline(cin, input))
	{
		std::cout << input << std::endl;
		std:: vector<std::string> tokens;
		split_str(input, ' ', tokens);
		// handle only the login command to check that the connection handler is established
		if(tokens.at(0) == "login"){
			handle_login(tokens.at(1), tokens.at(2), tokens.at(3));
		}
		
		try {
			if (!stompProtocol.handleCommand(input , *connectionHandler))
			{
				std::cerr << "Failed to handle command: " << input << std::endl;
			}
		}
		catch (const std::exception &e)
		{
			std::cerr << "Error: " << e.what() << std::endl;
		}
	}
};

void StompClient::listenToServer()
{
	std::string response;
	while (isRunning && connectionHandler->getFrameAscii(response, '\0'))
	{
		std::lock_guard<std::mutex> lock(mtx); // TODO check if to move mutex for better performance which data structures need protection and when ?
		stompProtocol.processResponse(response);
		response.clear();
	}
	std::cerr << "Disconnected from server. Exiting listener thread." << std::endl;
	isRunning = false;
}

void StompClient::stop()
{
	if (isRunning)
	{
		isRunning = false;
		connectionHandler->close();
		if (listenerThread.joinable())
		{
			listenerThread.join();
		}
	}
}

void StompClient::handle_login(const string& host, const string& username, const string& password)
{
	if (isConnected)
	{
		std::cerr << "Already connected to server." << std::endl;
		return;
	}
	std::vector<std::string> tokens;
	split_str(host, ':', tokens);
	const char *host_ip = tokens.at(0).c_str();
	short port = (short)stoi(tokens.at(1));

	if (!connectionHandler){
		connectionHandler = new ConnectionHandler(host_ip, port);
	}
	
	if (!connectionHandler->connect())
	{
		std::cerr << "Failed to connect to server." << std::endl;
		return;
	}
}

int main(int argc, char *argv[])
{
	StompClient client;
	client.run();
	return 0;
}
