#import "RNDocumentPicker.h"

#if __has_include(<React/RCTConvert.h>)
#import <React/RCTConvert.h>
#import <React/RCTBridge.h>
#import <React/RCTUtils.h>
#else // back compatibility for RN version < 0.40
#import "RCTConvert.h"
#import "RCTBridge.h"
#endif

#define IDIOM    UI_USER_INTERFACE_IDIOM()
#define IPAD     UIUserInterfaceIdiomPad

@interface RNDocumentPicker()

@property (nonatomic, strong) RCTResponseSenderBlock callback;

@end

@implementation RNDocumentPicker

@synthesize bridge = _bridge;

- (instancetype)init
{
    if ((self = [super init])) {
    }

    return self;
}

- (dispatch_queue_t)methodQueue
{
    return dispatch_get_main_queue();
}

RCT_EXPORT_MODULE();

RCT_EXPORT_METHOD(show:(NSDictionary *)options
                  callback:(RCTResponseSenderBlock)callback) {

    NSArray *allowedUTIs = [RCTConvert NSArray:options[@"filetype"]];
    UIDocumentMenuViewController *documentMenu = [[UIDocumentMenuViewController alloc] initWithDocumentTypes:(NSArray *)allowedUTIs inMode:UIDocumentPickerModeImport];

    self.callback = callback;

    documentMenu.delegate = self;
    documentMenu.modalPresentationStyle = UIModalPresentationFormSheet;

    UIViewController *rootViewController = RCTPresentedViewController();

    if ( IDIOM == IPAD ) {
        NSNumber *top = [RCTConvert NSNumber:options[@"top"]];
        NSNumber *left = [RCTConvert NSNumber:options[@"left"]];
        [documentMenu.popoverPresentationController setSourceRect: CGRectMake([left floatValue], [top floatValue], 0, 0)];
        [documentMenu.popoverPresentationController setSourceView: rootViewController.view];
    }

    [rootViewController presentViewController:documentMenu animated:YES completion:nil];
}


- (void)documentMenu:(UIDocumentMenuViewController *)documentMenu didPickDocumentPicker:(UIDocumentPickerViewController *)documentPicker {
    documentPicker.delegate = self;
    documentPicker.modalPresentationStyle = UIModalPresentationFormSheet;

    UIViewController *rootViewController = RCTPresentedViewController();

    if ( IDIOM == IPAD ) {
        [documentPicker.popoverPresentationController setSourceRect: CGRectMake(rootViewController.view.frame.size.width/2, rootViewController.view.frame.size.height - rootViewController.view.frame.size.height / 6, 0, 0)];
        [documentPicker.popoverPresentationController setSourceView: rootViewController.view];
    }

    [rootViewController presentViewController:documentPicker animated:YES completion:nil];
}

- (void)documentPicker:(UIDocumentPickerViewController *)controller didPickDocumentAtURL:(NSURL *)url {
    if (controller.documentPickerMode == UIDocumentPickerModeImport) {
        [url startAccessingSecurityScopedResource];

        NSFileCoordinator *coordinator = [[NSFileCoordinator alloc] init];
        __block NSError *error;

        [coordinator coordinateReadingItemAtURL:url options:NSFileCoordinatorReadingResolvesSymbolicLink error:&error byAccessor:^(NSURL *newURL) {
            NSMutableDictionary* result = [NSMutableDictionary dictionary];

            [result setValue:newURL.absoluteString forKey:@"uri"];
            [result setValue:[newURL lastPathComponent] forKey:@"fileName"];

            NSError *attributesError = nil;
            NSDictionary *fileAttributes = [[NSFileManager defaultManager] attributesOfItemAtPath:newURL.path error:&attributesError];
            if(!attributesError) {
                [result setValue:[fileAttributes objectForKey:NSFileSize] forKey:@"fileSize"];
            } else {
                NSLog(@"%@", attributesError);
            }

            self.callback(@[[NSNull null], result]);
        }];

        [url stopAccessingSecurityScopedResource];
    }
}

@end
