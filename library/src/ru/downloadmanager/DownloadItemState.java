package ru.downloadmanager;


enum DownloadItemState {
    NEW,  // initial state
    STARTED,  // started downloading
    STOPPED,  // completed by 'stop' call
    ERROR,  // completed with error
    DONE  //success complete
}
