package com.bluetooth.le;

import android.util.Log;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by caoxuanphong on 1/3/18.
 */

public class RequestHandler {
    private static final String TAG = "RequestHandler";
    private Queue<Request> requestQueue = new ConcurrentLinkedQueue<>();
    private ReentrantLock lock = new ReentrantLock();

    public synchronized void enqueue(Request request) {
        lock.lock();
        requestQueue.add(request);
        lock.unlock();
    }

    public synchronized void reset() {
        lock.lock();
        requestQueue.clear();
        lock.unlock();
    }

    public synchronized void impl(FioTBluetoothLE le) {
        lock.lock();

        if (le == null) {
            Log.e(TAG, "impl: le is null");
            return;
        }

        if (requestQueue.size() == 0) {
            Log.d(TAG, "impl: empty request queue");
            return;
        }

        Request request = requestQueue.element();
        RequestData requestData = request.getData();
        switch (request.getCmd()) {
            case READ:
                le.requestCharacteristicValue(requestData.getCharacteristic());
                break;

            case WRITE:
                le.writeToCharacteristic(requestData.getCharacteristic(),
                        requestData.getData());
                break;

            default:
                break;
        }

        lock.unlock();
    }

    public synchronized void implRightNow(FioTBluetoothLE le) {
        if (requestQueue.size() == 1) {
            impl(le);
        } else {
            Log.d(TAG, "Not implement due to request queue size is: " + requestQueue.size());
        }
    }

    public synchronized void dequeue() {
        lock.lock();
        requestQueue.remove();
        lock.unlock();
    }

}
