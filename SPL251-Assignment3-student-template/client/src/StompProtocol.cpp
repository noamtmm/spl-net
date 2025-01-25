#include "../include/StompProtocol.h"
#include <fstream>

StompProtocol::StompProtocol() : username(""), subscriptions(), logged_in(false), connection_id(-1), msg_id(0) {}

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

int StompProtocol::getAndIncreaseMsgId()
{
    int temp = msg_id;
    msg_id++;
    return temp;
}
int StompProtocol::getMsgId() 
{
    return msg_id;
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

        size_t seperator = host.find(':');
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
        headers["host"] = host+":"+port;
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
        headers["id"] = to_string(connection_id);      // check what id is needed to be sent and replace it
        headers["receipt"] = to_string(getMsgId()); // check what receipt is needed to be sent and replace it
        msg_id++;
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
        headers["receipt"] = to_string(getMsgId()); // check what receipt is needed to be sent and replace it
        msg_id++;
        string frame = createFrame("UNSUBSCRIBE", headers);

        if (connectionHandler.sendFrameAscii(frame, '\0'))
        {
            subscriptions.erase(channel_name);
            return true;
        };

        return false;
    }
    else if (cmd == "report") {

        string filename;
        ss >> filename;
        
        // Read file
        std::ifstream file(filename);
        if (!file.is_open()) {
            cout << "Could not open file" << endl;
            return false;
        }
        
        // Parse JSON and create SEND frame
        map<string, string> headers;
        headers["destination"] = "/topic/name"; // Replace with actual topic
        string frame = createFrame("SEND", headers);
        return connectionHandler.sendFrameAscii(frame, '\0');
    }
    else if (cmd == "summary") {
        string channel, username, filename;
        ss >> channel >> username >> filename;
        // TODO: Implement summary logic
        return true;
    }
    else if (cmd == "logout") {
        map<string, string> headers;
        headers["receipt"] = to_string(msg_id); 
        string frame = createFrame("DISCONNECT", headers);
        logged_in = false;
        return connectionHandler.sendFrameAscii(frame, '\0');
    }

    cout << "Invalid command" << endl; 
    return false;
}

int StompProtocol::getId() 
{
    return connection_id;
}

void StompProtocol::setId(int id)
{
    connection_id = id;
}

void StompProtocol::processResponse(const string &response)
{
    stringstream ss(response);
    string cmd;

    ss >> cmd;
    if (cmd != "ID" && cmd != "RECEIPT")
    {
        cout << response << endl; // For debugging purposes
    }

    if (cmd == "CONNECTED")
    {
        logged_in = true;
        cout << "Login successful" << endl;
    }

    else if (cmd == "ID")
    {
        // Skip the rest of the first line (after the first word)
        string dummy;
        getline(ss, dummy);  // Read until the end of the first line

        // Now process the second line
        string secondLine;
        getline(ss, secondLine);  // Read the second line
        
        // Extract the second word from the second line
        stringstream secondLineStream(secondLine);
        string word1, word2;
        secondLineStream >> word1 >> word2;  // Extract first and second word
        
        cout << "connection id from server: " + word2 << endl;  // Print the second word
        setId(std::stoi(word2));
    }
    else if (cmd == "RECEIPT")
    {        
        cout << response << endl;
    }

    
}