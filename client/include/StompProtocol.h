#pragma once

#include "../include/ConnectionHandler.h"
#include "../include/event.h"

#include <sstream>
#include <iostream>
#include <vector>

using namespace std;

// TODO: implement the STOMP protocol
class StompProtocol
{
private:
    std::string username;
    std::map<std::string, std::string> subscriptions;
    bool logged_in;

    std::string createFrame(const string &command, const map<string, string> &headers, const string &body);

public:
    StompProtocol();
    ~StompProtocol();

    bool handleCommand(const string &command, ConnectionHandler &connectionHandler);
    void processResponse(const string &response); // maybe adding ConnectionHandler as a parameter if needed
    void setLoggedIn(bool loggedIn) { logged_in = loggedIn; }
    bool getLoggedIn() { return logged_in; }
};
