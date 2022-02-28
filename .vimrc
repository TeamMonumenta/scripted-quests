" Remove whitespace from end of lines when saving
autocmd BufWritePre * :%s/\s\+$//e

" Use tabs in Java files
autocmd Filetype java set noexpandtab

" 4-width spaces everywhele else
set expandtab
set shiftwidth=4
set tabstop=4
