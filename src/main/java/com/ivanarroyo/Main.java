package com.ivanarroyo;

import com.ivanarroyo.commands.Command;
import com.ivanarroyo.commands.AddCommand;
import com.ivanarroyo.commands.InitCommand;
import com.ivanarroyo.commands.StatusCommand;
import com.ivanarroyo.commands.CommitCommand;
import com.ivanarroyo.commands.BranchCommand;
import com.ivanarroyo.commands.CheckoutCommand;
import com.ivanarroyo.commands.StashCommand;
import com.ivanarroyo.core.ObjectStore;

public class Main {
    public static void main(String[] args) {
        if(args.length == 0) {
            System.out.println("Usage: opipop <command>");
            return;
        }

        String cmd = args[0];

        ObjectStore store = new ObjectStore(".opipop");

        Command command;
        switch(cmd) {
            case "init":
                command = new InitCommand(store);
                break;
            case "add":
                command = new AddCommand(store);
                break;
            default:
                System.out.println("Unknown command: " + cmd);
                return;
        }

        String[] commandArgs = new String[Math.max(0, args.length - 1)];
        if (args.length > 1) {
            System.arraycopy(args, 1, commandArgs, 0, args.length - 1);
        }

        try {
            command.execute(commandArgs);
        } catch (Exception e) {
            System.out.println("Error executing command: " + e.getMessage());
            e.printStackTrace(System.err);
        }
    }
}
