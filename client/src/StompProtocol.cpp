#include "../include/StompProtocol.h"

StompProtocol::StompProtocol() : logged_in(false), username(""), subscriptions() {}

StompProtocol::~StompProtocol() {}

string StompProtocol::createFrame(const string &command, const map<string, string> &headers, const string &body = "")
{
    string frame = command + "\n";

    for (const auto &header : headers)
    {
        frame += header.first + ":" + header.second + "\n";
    }

    frame += "\n" + body + "\n";

    return frame;
}

bool StompProtocol::handleCommand(const string &input, ConnectionHandler &connectionHandler)
{
    stringstream ss(input);
    string cmd; // command name like login, subscribe , etc.
    ss >> cmd;

    
    if (cmd == "login")
    {
        if (logged_in){
            cout << "User already logged in" << endl;
            return false;
        }
        string host, port, username, password;
        ss >> host >> username >> password;

        int seperator = host.find(':');
        if (seperator == string::npos)
        {
            cout << "Invalid host:port format" << endl;
            return false;
        }

        port = host.substr(seperator + 1); // first get the port so host will not substring it
        host = host.substr(0, seperator);
        this->username = username;

        map<string, string> headers;
        headers["accept-version"] = "1.2";
        headers["host"] = host;
        headers["login"] = username;
        headers["passcode"] = password;

        string frame = createFrame("CONNECT", headers);

        return connectionHandler.sendFrameAscii(frame, '\0');
    }

    else if (cmd == "join")
    {
        string channel_name;
        ss >> channel_name;

        string id = to_string(subscriptions.size() + 1); // check what id is needed to be sent and replace it
        map<string, string> headers;
        headers["destination"] = "/" + channel_name;
        headers["id"] = to_string(1);      // check what id is needed to be sent and replace it
        headers["receipt"] = to_string(1); // check what receipt is needed to be sent and replace it

        string frame = createFrame("SUBSCRIBE", headers);

        if (connectionHandler.sendFrameAscii(frame, '\0'))
        {
            subscriptions[channel_name] = id;
            return true;
        };

        return false;
    }

    else if (cmd == "exit")
    {
        string channel_name;
        ss >> channel_name;

        if (subscriptions.find(channel_name) == subscriptions.end())
        {
            cout << "You are not subscribed to this channel" << endl;
            return false;
        }

        map<string, string> headers;
        headers["id"] = subscriptions[channel_name];
        headers["receipt"] = to_string(1); // check what receipt is needed to be sent and replace it

        string frame = createFrame("UNSUBSCRIBE", headers);

        if (connectionHandler.sendFrameAscii(frame, '\0'))
        {
            subscriptions.erase(channel_name);
            return true;
        };

        return false;
    }
    else if (cmd == "report")
    {

    }
    else if (cmd == "summary")
    {

    }
    else if (cmd == "logout")
    {
        
    }

    cout << "Invalid command" << endl; // TODO: check if certain output is needed
    return false;
}

void StompProtocol::processResponse(const string &response)
{
    cout << response << endl; // For debugging purposes
    stringstream ss(response);
    string cmd;

    ss >> cmd;

    if (cmd == "CONNECTED")
    {
        logged_in = true;
        cout << "Login successful" << endl;
    }

    else if (cmd == "RECEIPT")
    {
        // need to save a structure of requests and their corresponding receipts to check if the receipt is valid and to know what to do with it
        string receipt_id;
        ss >> receipt_id;
        cout << "Receipt id: " << receipt_id << endl;
    }

    else
    {
        // ADD LOGIC IF NEEDED
    }
}